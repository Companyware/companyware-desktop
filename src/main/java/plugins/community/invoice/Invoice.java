package plugins.community.invoice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import platform.api.persistence.PluginDatabase;
import platform.api.persistence.PluginMigration;
import platform.api.plugin.Plugin;
import platform.api.plugin.PluginContext;
import platform.api.ui.DesktopShell;
import platform.api.ui.PlatformServiceIds;
import plugins.community.invoice.infrastructure.JdbcInvoiceService;
import plugins.community.invoice.infrastructure.JdbcInvoiceDocumentService;
import plugins.community.invoice.infrastructure.JdbcEInvoiceService;
import plugins.community.invoice.infrastructure.JdbcInvoiceMailService;
import plugins.community.invoice.application.EInvoiceService;
import plugins.community.invoice.application.InvoiceMailService;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.view.InvoiceWorkspacePanel;
import platform.api.ui.CompanywareIcons;
import platform.api.security.SecretProvider;
import platform.api.security.MutableSecretProvider;
import platform.api.security.SecurityServiceIds;

/** Invoice plugin composition root for persistence, application service and desktop view. */
public final class Invoice implements Plugin {
    @Override public void initialize(PluginContext context) throws Exception {
        PluginDatabase database = context.database();
        database.migrate(context.descriptor().getId(), Arrays.asList(
                new PluginMigration(1, "create invoice domain tables", migration("createInvoiceTables.sql"),
                        "54f5a21511ab10f4c90d29393fd5c1ab27cdc0ee3237d19171055495b2401576"),
                new PluginMigration(2, "create invoice template table", migration("createInvoiceTemplateTable.sql")),
                new PluginMigration(3, "create invoice branding table", migration("createInvoiceBrandingTable.sql")),
                new PluginMigration(4, "add invoice letterhead fields", migration("addInvoiceLetterheadFields.sql")),
                new PluginMigration(5, "create immutable e-invoice archive", migration("createEInvoiceArchive.sql")),
                new PluginMigration(6, "create invoice mail tables", migration("createInvoiceMailTables.sql")),
                new PluginMigration(7, "create invoice SMTP settings", migration("createInvoiceSmtpSettings.sql")),
                new PluginMigration(8, "add SMTP sender display name", migration("addInvoiceSmtpFromName.sql"))));
        final InvoiceService invoices = new JdbcInvoiceService(database);
        final InvoiceDocumentService documents = new JdbcInvoiceDocumentService(database, invoices);
        final EInvoiceService eInvoices = new JdbcEInvoiceService(database, invoices, documents);
        final SecretProvider secrets = context.find(SecurityServiceIds.SECRETS)
                .orElseThrow(() -> new IllegalStateException("Secret provider is unavailable"));
        final MutableSecretProvider mutableSecrets = context.find(SecurityServiceIds.MUTABLE_SECRETS)
                .orElseThrow(() -> new IllegalStateException("Mutable secret provider is unavailable"));
        final InvoiceMailService mail = new JdbcInvoiceMailService(database, invoices, eInvoices, secrets, mutableSecrets);
        context.register(InvoiceServiceIds.INVOICES, invoices);
        context.register(InvoiceServiceIds.DOCUMENTS, documents);
        context.register(InvoiceServiceIds.E_INVOICES, eInvoices);
        context.register(InvoiceServiceIds.MAIL, mail);
        context.addLocalizedView("invoice.overview.view", "invoice.overview", 131,
                () -> new InvoiceWorkspacePanel(invoices, documents, eInvoices, mail, context.messages(),
                        InvoiceWorkspacePanel.InitialView.OVERVIEW));
        context.addLocalizedView("invoice.create.view", "invoice.new", 132,
                () -> new InvoiceWorkspacePanel(invoices, documents, eInvoices, mail, context.messages(),
                        InvoiceWorkspacePanel.InitialView.CREATE));
        context.addLocalizedView("invoice.template.view", "invoice.template", 133,
                () -> new InvoiceWorkspacePanel(invoices, documents, eInvoices, mail, context.messages(),
                        InvoiceWorkspacePanel.InitialView.TEMPLATE));
        context.addLocalizedView("invoice.branding.view", "invoice.branding", 134,
                () -> new InvoiceWorkspacePanel(invoices, documents, eInvoices, mail, context.messages(),
                        InvoiceWorkspacePanel.InitialView.BRANDING));
        context.addLocalizedView("invoice.search.view", "invoice.search", 135,
                () -> new InvoiceWorkspacePanel(invoices, documents, eInvoices, mail, context.messages(),
                        InvoiceWorkspacePanel.InitialView.SEARCH));
        context.addLocalizedView("invoice.mailTemplate.view", "invoice.mailTemplate", 136,
                () -> new InvoiceWorkspacePanel(invoices, documents, eInvoices, mail, context.messages(),
                        InvoiceWorkspacePanel.InitialView.MAIL_TEMPLATE));
        context.addLocalizedNavigationGroup("invoice.navigation.group", "invoice.title", 130);
        context.addLocalizedNavigation("invoice.navigation.overview", "invoice.overview", 131,
                "invoice.overview.view", CompanywareIcons.OVERVIEW);
        context.addLocalizedNavigation("invoice.navigation.create", "invoice.new", 132,
                "invoice.create.view", CompanywareIcons.CREATE);
        context.addLocalizedNavigation("invoice.navigation.template", "invoice.template", 133,
                "invoice.template.view", CompanywareIcons.EDIT);
        context.addLocalizedNavigation("invoice.navigation.branding", "invoice.branding", 134,
                "invoice.branding.view", CompanywareIcons.IMAGE);
        context.addLocalizedNavigation("invoice.navigation.search", "invoice.search", 135,
                "invoice.search.view", CompanywareIcons.SEARCH);
        context.addLocalizedNavigation("invoice.navigation.mailTemplate", "invoice.mailTemplate", 136,
                "invoice.mailTemplate.view", CompanywareIcons.MAIL);
        context.addLocalizedMenuItem("invoice.menu", "invoice.title", 130, "settings", () -> {
            DesktopShell shell = context.find(PlatformServiceIds.DESKTOP_SHELL)
                    .orElseThrow(() -> new IllegalStateException("Desktop shell is unavailable"));
            shell.showView(new InvoiceWorkspacePanel(invoices, documents, eInvoices, mail, context.messages(),
                    InvoiceWorkspacePanel.InitialView.OVERVIEW));
        });
        context.addLocalizedMenuItem("invoice.smtp.menu", "invoice.smtpSettings", 131, "settings", () -> {
            DesktopShell shell = context.find(PlatformServiceIds.DESKTOP_SHELL)
                    .orElseThrow(() -> new IllegalStateException("Desktop shell is unavailable"));
            shell.showView(new InvoiceWorkspacePanel(invoices, documents, eInvoices, mail, context.messages(),
                    InvoiceWorkspacePanel.InitialView.SMTP_SETTINGS));
        });
    }

    private String migration(String filename) throws IOException {
        String path = "plugins/community/invoice/migration/" + filename;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing migration resource: " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
