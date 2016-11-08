# EmbeddedLdapKDC

 This is the project for starting an embedded Directory Service and Kerberos which can be used to run locally or also
 can be used for JUNIT Testing for LDAP Integration use cases or Kerberos. 
 This is also useful in Hadoop testing to mimic scenerios of a kerbeorized cluster. 
 Also, I added few Microsoft AD schema object like sAMAccountName and memberOf to mimic Active Directory. 
 (One can look at /EmbeddedLdapKDC/src/main/resources/krish.schema)
 
 So, the EmbeddedLdapKDC is an eclipse project and can be used to run it inside elipse or debugging if you change any code.
 The main class is <b>com.krish.ead.server.EADServer</b>.
 
 
 For writing Jnit Test cases, refer to the class com.krish.ead.server.KerberosLdapIntegrationTest.
 A lot of time we need to generate keytabs on the fly as needed for Hadoop related integration Testing also.
 Refer to the method com.krish.ead.server.KerberosLdapIntegrationTest.createKeytab.

 We may want to kinit from unix shell if developing a shell script for various use cases like ldap search, or ldap modify etc.
 Copy the krb5.conf located here /EmbeddedLdapKDC/src/main/resources/krb5.conf
 and use this command. The default location in unix is <b>/etc/krb5.conf</b>.
 env KRB5_CONFIG=<<location of krb5.conf>> kinit krish
 
 You may need to programatically addUser or Groups or add users to the group.
 Refer to the class com.krish.directory.service.EadSchemaService, where various methods are exposed to do the same.
 
 Alternatively, you may also use ldapsearch, ldapmodify, ldapadd etc from unix shell or in a script.
 Refer to the command below to add an user. 
 
 bash>ldapmodify -H "ldap://localhost:10389" -D "cn=manager,ou=users,dc=jpmis,dc=com" -w manager -a -f captain.ldif
 
 
Note: By deafult, the embedded server by default will add users as Read only user, so if you need to add, delete or modify any objects 
one need to be login as  cn=manager,ou=users,dc=jpmis,dc=com with password manager to do so,

The default port for LDAP Server is 10389 and Kerberos is 16088. Feel free to change this.

You can also start the server from shell.
Go to StandaloneEmbeddedLdapKDC/bin and invoke ./ead.sh <<instance name>> <<start or run>> 


In case, you are testing Kerberos with any Hadoop related use case, you may want to go through
http://krishdey5.blogspot.com/

