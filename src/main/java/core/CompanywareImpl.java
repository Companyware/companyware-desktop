/*******************************************************************************
 * MIT License
 *
 * Companyware - a java desktop framework for plugins
 *
 * Copyright (c) 2023 mbdus-Softwareentwicklung - Mathias Bauer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.xml.sax.SAXException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import models.plugin.PluginModel;
import models.settings.SettingsModel;
import models.user.UserModel;
import platform.api.ui.DesktopShell;
import platform.api.ui.PlatformServiceIds;
import pluginmanager.plugininterfaces.PluginManager;
import models.settings.Repository;

public class CompanywareImpl {
	
	public List<Object[]> getPluginSettings(String pluginName) throws ParserConfigurationException, SAXException, IOException{
		CompanywareHelper ch = new CompanywareHelper(); 
		ch.clearXmlSettings(pluginName);
		
		try (Session session = HibernateUtils.getSessionFactory().openSession()) {
			Query query= session.createQuery(""
		        		+ "from PluginModel pm "
		        		+ "join PluginSettingsModel sm "
		        		+ "on pm.id = sm.pluginId "
		        		+ "where pm.name=:name "
		        		+ "");
    	List<Object[]> settings;
		query.setParameter("name", pluginName);
			settings = query.getResultList();
			return settings;
		}
	}
	
	public PluginModel getPluginByName(String pluginName){
		try (Session session = HibernateUtils.getSessionFactory().openSession()) {
			Query query= session.createQuery(""
		        		+ "from PluginModel pm "
		        		+ "where pm.name=:name "
		        		+ "");
		PluginModel plugin;
		query.setParameter("name", pluginName);
			plugin = (PluginModel) query.getSingleResult();
			return plugin;
		}
	}
	
	public String getVersionNumber(){
		String version = "";
		try (Session session = HibernateUtils.getSessionFactory().openSession()) {
			Query query= session.createQuery(""
		        		+ "from SettingsModel s "
		        		+ "where s.name=:name "
		        		+ "");
		SettingsModel settings = null;
		query.setParameter("name", "version");
		try {
			settings = (SettingsModel) query.getSingleResult();
		} catch (Exception e) {
			// TODO: handle exception
		}
			if(settings != null){
				version = settings.getValue();
			}
		}
		return version;
	}
	
	public void checkUpdate(){
		SettingsModel settings = null;
		try (Session session = HibernateUtils.getSessionFactory().openSession()) {
			Query query= session.createQuery(""
		        		+ "from SettingsModel s "
		        		+ "where s.name=:name "
		        		+ "");
			query.setParameter("name", "version");
			try {
				settings = (SettingsModel) query.getSingleResult();
			} catch (Exception ignored) {
				// A missing version row is initialized from the packaged properties below.
			}
		}
		
		DefaultArtifactVersion artifactDbVersion = null;
		String dbVersion = "";
		if(settings != null){
			dbVersion = settings.getValue();
			artifactDbVersion = new DefaultArtifactVersion(dbVersion);
		}
		
		Properties appProps = new Properties();
		try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
			if (input == null) throw new IOException("Missing application.properties");
			appProps.load(input);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read application version", e);
		}
		String fileVersion = appProps.getProperty("companyware.version");
		DefaultArtifactVersion artifactFileVersion = new DefaultArtifactVersion(fileVersion);
		
		if(dbVersion.isEmpty() && fileVersion != null){
			settings = new SettingsModel();
			settings.setName("version");
			settings.setValue(fileVersion);
			Repository.save(settings);
			artifactDbVersion = new DefaultArtifactVersion(fileVersion);
		}
		
		if (artifactDbVersion.compareTo(artifactFileVersion)<0){
			settings.setName("version");
			settings.setValue(fileVersion);
			Repository.save(settings);
			//update
			this.executeUpdateSql(fileVersion);
		}
	}
	
	public void checkFirstRun(){
		// First-run state belongs to the selected database, not to a local file.
		try {
			core.security.SecuritySchemaMigration.migrate();
		} catch (SQLException failure) {
			throw new IllegalStateException("Identity schema migration failed", failure);
		}
		this.setFirstRun();
	}
	
	public void setFirstRun(){
		SettingsModel settings = null;
		try (Session session = HibernateUtils.getSessionFactory().openSession()) {
			Query query= session.createQuery(""
		        		+ "from SettingsModel s "
		        		+ "where s.name=:name "
		        		+ "");
			query.setParameter("name", "firstRun");
			try {
				settings = (SettingsModel) query.getSingleResult();
			} catch (Exception ignored) {
				// A missing first-run row is initialized below.
			}
		}
		boolean insertData = false;
		if(settings == null){
			settings = new SettingsModel();
			settings.setName("firstRun");
			settings.setValue("1");
			insertData = true;
		}
		else{
			settings.setValue("0");
		}
		Repository.save(settings);
		if(insertData){
			this.executeSetupSql();
			this.insertData();
		}
	}
	
	public void insertData(){
		UserModel user = new UserModel();
		user.setName("companyware");
		user.setUsername("companyware");
		user.setEmail("info@companyware.de");
		user.setActive(true);
		user.setPasswordChangeRequired(true);
		user.setRoles("ADMIN");
		user.setPermissions("*");
		String encryptedPassword = this.encoder().encode("companyware");
		user.setPassword(encryptedPassword);
		models.user.Repository.save(user);
	}
	
	public void executeSetupSql(){
		executeSqlResource("setup/install.sql", true);
	}
	
	public void executeUpdateSql(String fileVersion){
		executeSqlResource("setup/update"+fileVersion+".sql", false);
	}

	private void executeSqlResource(String resource, boolean required) {
		InputStream input = getClass().getClassLoader().getResourceAsStream(resource);
		if (input == null && !required) return;
		if (input == null) throw new IllegalStateException("Missing SQL resource: " + resource);
		try (InputStream stream = input;
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
				Session session = HibernateUtils.getSessionFactory().openSession()) {
			StringBuilder statement = new StringBuilder();
			for (String line; (line = reader.readLine()) != null;) {
				statement.append(line.trim().endsWith(";") ? line.substring(0, line.lastIndexOf(';')) : line);
				if (line.trim().endsWith(";")) {
					Transaction transaction = session.beginTransaction();
					try {
						session.createNativeQuery(statement.toString()).executeUpdate();
						transaction.commit();
					} catch (RuntimeException failure) {
						if (transaction.isActive()) transaction.rollback();
						throw failure;
					}
					statement.setLength(0);
				} else {
					statement.append(' ');
				}
			}
		} catch (IOException failure) {
			throw new IllegalStateException("Unable to execute SQL resource: " + resource, failure);
		}
	}
	
	public BCryptPasswordEncoder encoder() {
	    return new BCryptPasswordEncoder();
	}
	
	public void setSuccessMessage(String message, PluginManager pm){
		DesktopShell desktopShell = pm.getService(PlatformServiceIds.DESKTOP_SHELL).orElse(null);
		if (desktopShell != null) {
			desktopShell.showMessage(message);
		}
	}
}
