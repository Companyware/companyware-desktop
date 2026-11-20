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

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import persistence.PersistenceSettings;

public class HibernateUtils {

    private HibernateUtils() {}

    private static final SessionFactory sessionFactory;

    static {
        try {
			Configuration config = new Configuration().configure("hibernate.cfg.xml");
			PersistenceSettings.load().applyTo(config);
			// Explicit classpath mappings work identically from Eclipse, Maven test
			// classes and the nested Spring Boot executable JAR. Filesystem scanning
			// cannot see BOOT-INF/classes inside the packaged application.
			config.addResource("models/plugin/PluginModel.hbm.xml");
			config.addResource("models/pluginsettings/PluginSettingsModel.hbm.xml");
			config.addResource("models/settings/SettingsModel.hbm.xml");
			config.addResource("models/user/UserModel.hbm.xml");

			// Schema presence is database state, never notebook-local lock-file state.
			// Versioned data changes are coordinated by MigrationRunner.
	        config.setProperty("hibernate.hbm2ddl.auto", "update");
	        config.setProperty("hbm2ddl.auto", "update");
       // 	config.setProperty("hbm2ddl.import_files", "tables.sql");
            sessionFactory = config.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            System.out.println(ex.getMessage());
            System.out.println(ex.getStackTrace().toString());
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
    public static void shutdown() throws Exception {
		SessionFactory factory = getSessionFactory();
		if (!factory.isClosed()) factory.close();
    }
}
