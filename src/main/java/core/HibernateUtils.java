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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
 	        
 	        URL url = HibernateUtils.class.getResource("/");
 	        URI uri = null;
 			try {
 				if(url!=null){
 					uri = url.toURI();
 				}
 			} catch (URISyntaxException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 	     
			File file;
			if (uri != null && uri.getScheme().equals("jar")) {
				file = new File("classes");
			}
			else{
				file = new File("src/main/java");
			}
 			config.addDirectory(file);

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
