package com.krish.ead.server;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.krish.directory.service.EadSchemaService;

public class KerberosLdapIntegrationTest {
  private static DirectoryService directoryService;
  private static EadSchemaService eadSchemaService;
  private static File workDir;
  public static final String JAVA_SECURITY_KRB5_CONF = "java.security.krb5.conf";

  @BeforeClass
  public static void setUp() throws Exception {
    EADServer.start("/tmp/krish", 10389);
    directoryService = EADServer.getDirectoryService();
    eadSchemaService = new EadSchemaService(directoryService);
    eadSchemaService.createUser("krishdey", "krishdey");
    eadSchemaService.createGroup("ND-POC-ENG1");
    createTestDir();
  }

  @Test
  public void testAddUsertoGroup() throws Exception {

    eadSchemaService.addUserToGroup("krishdey", "ND-POC-ENG1");
    assertTrue(eadSchemaService.checkIfUserExist("krishdey"));
    assertTrue(eadSchemaService.checkIfGroupExist("ND-POC-ENG1"));
    assertTrue(eadSchemaService.checkIfUserMemberOfGroup("krishdey", "ND-POC-ENG1"));
  }

  private static class KerberosConfiguration extends Configuration {
    private String principal;
    private String keytab;
    private boolean isInitiator;

    private KerberosConfiguration(String principal, File keytab, boolean client) {
      this.principal = principal;
      this.keytab = keytab.getAbsolutePath();
      this.isInitiator = client;
    }

    public static Configuration createClientConfig(String principal, File keytab) {
      return new KerberosConfiguration(principal, keytab, true);
    }

    public static Configuration createServerConfig(String principal, File keytab) {
      return new KerberosConfiguration(principal, keytab, false);
    }

    private static String getKrb5LoginModuleName() {
      return System.getProperty("java.vendor").contains("IBM") ? "com.ibm.security.auth.module.Krb5LoginModule"
          : "com.sun.security.auth.module.Krb5LoginModule";
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
      Map<String, String> options = new HashMap<String, String>();
      options.put("principal", principal);
      options.put("refreshKrb5Config", "true");
      options.put("keyTab", keytab);
      options.put("useKeyTab", "true");
      options.put("storeKey", "true");
      options.put("doNotPrompt", "true");
      options.put("useTicketCache", "true");
      options.put("renewTGT", "true");
      options.put("isInitiator", Boolean.toString(isInitiator));

      String ticketCache = System.getenv("KRB5CCNAME");
      if (ticketCache != null) {
        options.put("ticketCache", ticketCache);
      }
      options.put("debug", "true");

      return new AppConfigurationEntry[] { new AppConfigurationEntry(getKrb5LoginModuleName(),
          AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options) };
    }
  }

 

  public void setKrb5conf() throws IOException {
    String krb5String =
        IOUtils.toString(getClass().getClassLoader().getResourceAsStream("krb5.conf"));
    File krb5File = new File(workDir, "krb5.conf");
    FileOutputStream out = new FileOutputStream(krb5File);
    IOUtils.write(krb5String, out);
    IOUtils.closeQuietly(out);
    System.setProperty(JAVA_SECURITY_KRB5_CONF, krb5File.getAbsolutePath());
    System.setProperty("sun.security.krb5.debug", "true");
  }


  public static void createTestDir() throws IOException {
    workDir = new File("target", Long.toString(System.currentTimeMillis()));
    workDir.mkdir();
  }

  @SuppressWarnings("rawtypes")
  public static void createKeytab(File keytabFile, String principal, String passwd, String realm)
      throws Exception {

    Keytab keytab = new Keytab();
    List<KeytabEntry> entries = new ArrayList<KeytabEntry>();
    principal = principal + "@" + realm;
    KerberosTime timestamp = new KerberosTime();
    for (Map.Entry entry : KerberosKeyFactory.getKerberosKeys(principal, passwd).entrySet()) {
      EncryptionKey ekey = (EncryptionKey) entry.getValue();
      byte keyVersion = (byte) ekey.getKeyVersion();
      entries.add(new KeytabEntry(principal, (int) 1L, timestamp, keyVersion, ekey));

    }
    keytab.setEntries(entries);
    keytab.write(keytabFile);
  }
  
  
  @Test
  public void testKerberos() throws Exception {
    File keytab = new File(workDir+ "/krishdey.keytab");
    keytab.createNewFile();
    createKeytab(keytab, "krishdey", "krishdey", "EXAMPLE.COM");
    LoginContext loginContext = null;
    setKrb5conf();
    try {
      String principal = "krishdey@EXAMPLE.COM";

      Set<Principal> principals = new HashSet<Principal>();
      principals.add(new KerberosPrincipal(principal));

      // client login
      Subject subject =
          new Subject(false, principals, new HashSet<Object>(), new HashSet<Object>());
      loginContext =
          new LoginContext("", subject, null, KerberosConfiguration.createClientConfig(principal,
              keytab));
      loginContext.login();
      subject = loginContext.getSubject();
      
      Assert.assertEquals(1, subject.getPrincipals().size());
      Assert.assertEquals(KerberosPrincipal.class, subject.getPrincipals().iterator().next()
          .getClass());
      Assert.assertEquals(principal, subject.getPrincipals().iterator()
          .next().getName());
      loginContext.logout();

      // server login
      subject = new Subject(false, principals, new HashSet<Object>(), new HashSet<Object>());
      loginContext =
          new LoginContext("", subject, null, KerberosConfiguration.createServerConfig(principal,
              keytab));
      loginContext.login();
      subject = loginContext.getSubject();
      Assert.assertEquals(1, subject.getPrincipals().size());
      Assert.assertEquals(KerberosPrincipal.class, subject.getPrincipals().iterator().next()
          .getClass());
      Assert.assertEquals(principal, subject.getPrincipals().iterator()
          .next().getName());
      loginContext.logout();

    } finally {
      if (loginContext != null) {
        loginContext.logout();
      }
    }
  }
  

  @AfterClass
  public  static void tearDown() throws IOException {
    FileUtils.deleteDirectory(workDir);
    EADServer.stop();
  }

}
