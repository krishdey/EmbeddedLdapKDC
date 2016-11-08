package com.krish.directory.service;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author krishdey
 *
 */
public class EadSchemaService {

  /**Logger of the class */
  private static final Logger LOG = LoggerFactory.getLogger(EadSchemaService.class);
  
  /** Directory service of the EAD Server */
  private DirectoryService directoryService;

  public EadSchemaService(DirectoryService directoryService) {
    this.directoryService = directoryService;
  }

  /**
   * Add User
   * 
   * @param uid
   * @param password
   * @return Dn
   * @throws Exception
   */
  public Dn createUser(String uid, String password) throws Exception {
    LOG.info("Creating user with Cn: " + uid);
    Entry entry = new DefaultEntry(
          //@formatter:off
          directoryService.getSchemaManager(),
          "cn=" + uid + ",ou=users,dc=jpmis,dc=com",
          "uid", uid,
          "objectClass: user",
          "objectClass: person",
          "objectClass: organizationalPerson",
          "objectClass: inetOrgPerson",
          "objectClass: krb5principal",
          "objectClass: krb5kdcentry",
          "sn", uid,
          "cn", uid,
          "sAMAccountName",uid,
          "userPassword", password,
          "krb5PrincipalName", uid+"@EXAMPLE.COM",
          "krb5KeyVersionNumber", "0" );
          //@formatter:on
    directoryService.getAdminSession().add(entry);

    LOG.info("Created user in the EAD " + uid);
    return entry.getDn();
  }

  /**
   * Creates a simple groupOfUniqueNames under the ou=groups,dc=jpmis,dc=com
   * container. The admin user is always a member of this newly created group.
   *
   * @param groupName the name of the cgroup to create
   * @return the Dn of the group as a Name object
   * @throws Exception if the group cannot be created
   */
  public Dn createGroup(String groupName) throws Exception {
    LOG.info("Creating Group with : " + groupName);

    Dn groupDn = new Dn("cn=" + groupName + ",ou=groups,dc=jpmis,dc=com");

    Entry entry = new DefaultEntry(
          //@formatter:off
          directoryService.getSchemaManager(),
          "cn=" + groupName + ",ou=groups,dc=jpmis,dc=com",
          "objectClass: top",
          "objectClass: groupOfUniqueNames",
          "objectClass: group",
          "uniqueMember: uid=admin, ou=system",
          "cn", groupName );
          //@formatter:on
    directoryService.getAdminSession().add(entry);

    return groupDn;
  }

  /**
   * Adds an existing user under ou=users,dc=jpmis,dc=com to an existing group
   * under the ou=groups,dc=jpmis,dc=com container.
   *
   * @param userUid the uid of the user to add to the group
   * @param groupCn the cn of the group to add the user to
   * @throws Exception if the group does not exist
   */
  public void addUserToGroup(String userUid, String groupCn) throws Exception {
    LOG.info("Adding user with Cn: " + userUid + " to group " + groupCn);

    ModifyRequest modReq = new ModifyRequestImpl();
    modReq.setName(new Dn(directoryService.getSchemaManager(), "cn=" + groupCn
        + ",ou=groups,dc=jpmis,dc=com"));
    modReq.add("member", "cn=" + userUid + ",ou=users,dc=jpmis,dc=com");
    directoryService.getAdminSession().modify(modReq);

    modReq = new ModifyRequestImpl();
    modReq.setName(new Dn(directoryService.getSchemaManager(), "cn=" + userUid
        + ",ou=users,dc=jpmis,dc=com"));
    modReq.add("memberOf", "cn=" + groupCn + ",ou=groups,dc=jpmis,dc=com");
    directoryService.getAdminSession().modify(modReq);
  }

  /**
   * Removes a user from a group.
   *
   * @param userUid the Rdn attribute value of the user to remove from the group
   * @param groupCn the Rdn attribute value of the group to have user removed
   *          from
   * @throws Exception if there are problems accessing the group
   */
  public void removeUserFromGroup(String userUid, String groupCn) throws Exception {
    ModifyRequest modReq = new ModifyRequestImpl();
    modReq.setName(new Dn("cn=" + groupCn + ",ou=groups,dc=jpmis,dc=com"));
    modReq.remove("member", "cn=" + userUid + ",ou=users,dc=jpmis,dc=com");
    directoryService.getAdminSession().modify(modReq);
    
    modReq = new ModifyRequestImpl();
    modReq.setName(new Dn("cn=" + userUid + ",ou=users,dc=jpmis,dc=com"));
    modReq.remove("memberOf", "cn=" + groupCn + ",ou=groups,dc=jpmis,dc=com");
    directoryService.getAdminSession().modify(modReq);
  }

  /**
   * Check if the user exist
   * @param userUid
   * @return
   * @throws Exception
   */
  public boolean checkIfUserExist(String userUid) throws Exception {
    Dn userDn = new Dn("cn=" + userUid + ",ou=users,dc=jpmis,dc=com");
    return directoryService.getAdminSession().exists(userDn);
  }

  /**
   * Check if the group exist
   * @param groupCn
   * @return
   * @throws Exception
   */
  public boolean checkIfGroupExist(String groupCn) throws Exception {
    Dn groupDn = new Dn("cn=" + groupCn + ",ou=groups,dc=jpmis,dc=com");
    return directoryService.getAdminSession().exists(groupDn);
  }

  /**
   * Check if the user member of the group
   * @param userUid
   * @param groupCn
   * @return
   * @throws Exception
   */
  public boolean checkIfUserMemberOfGroup(String userUid, String groupCn) throws Exception {
    Dn userDn = new Dn("cn=" + userUid + ",ou=users,dc=jpmis,dc=com");
    Entry entry = directoryService.getAdminSession().lookup(userDn, "memberOf");
    Attribute attr = entry.get("memberOf");
    if (attr == null) {
      return false;
    }
    return attr.contains("cn=" + groupCn + ",ou=groups,dc=jpmis,dc=com");
  }
  
  /**
   * @throws Exception
   */
  public void loadTestUser() throws Exception {
    createUser("krishdey", "krishdey");
    createGroup("ND-DEY-ENG");
    addUserToGroup("krishdey", "ND-DEY-ENG");
  }

}
