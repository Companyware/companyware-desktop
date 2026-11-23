; Companyware
; Copyright (c) 2026 Companyware contributors
;
; SPDX-License-Identifier: MIT
; Licensed under the MIT License; see LICENSE in the project root.
; This file is part of Companyware.

#ifndef MyAppVersion
  #error MyAppVersion must be provided by tools/build-windows-installer.ps1
#endif
#ifndef SourceDir
  #error SourceDir must be provided by tools/build-windows-installer.ps1
#endif
#ifndef RuntimeDir
  #error RuntimeDir must be provided by tools/build-windows-installer.ps1
#endif
#ifndef InstallerOutputDir
  #error InstallerOutputDir must be provided by tools/build-windows-installer.ps1
#endif

[Setup]
AppId={{7DCE5A54-4E7B-4E8D-A9A0-77B1B02A20F6}
AppName=Companyware Desktop
AppVersion={#MyAppVersion}
AppVerName=Companyware Desktop {#MyAppVersion}
AppPublisher=Companyware
AppPublisherURL=https://www.companyware.de/
AppSupportURL=https://github.com/Companyware/companyware-desktop/issues
AppUpdatesURL=https://github.com/Companyware/companyware-desktop/releases/latest
DefaultDirName={localappdata}\Programs\Companyware Desktop
DefaultGroupName=Companyware Desktop
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir={#InstallerOutputDir}
OutputBaseFilename=Companyware-Setup
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x86compatible x64compatible
CloseApplications=yes
RestartApplications=no
Uninstallable=yes

[Languages]
Name: "german"; MessagesFile: "compiler:Languages\German.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Dirs]
Name: "{userappdata}\Companyware"

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#RuntimeDir}\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#RuntimeDir}\..\LICENSE"; DestDir: "{app}\runtime"; DestName: "TEMURIN-LICENSE"; Flags: ignoreversion skipifsourcedoesntexist
Source: "{#RuntimeDir}\..\ASSEMBLY_EXCEPTION"; DestDir: "{app}\runtime"; DestName: "TEMURIN-ASSEMBLY-EXCEPTION"; Flags: ignoreversion skipifsourcedoesntexist
Source: "{#RuntimeDir}\..\THIRD_PARTY_README"; DestDir: "{app}\runtime"; DestName: "TEMURIN-THIRD-PARTY-README"; Flags: ignoreversion skipifsourcedoesntexist

[Icons]
Name: "{group}\Companyware Desktop"; Filename: "{app}\runtime\bin\javaw.exe"; Parameters: "-Dcompanyware.persistence.embedded.path=""{userappdata}\Companyware\data\companyware"" -Dcompanyware.plugins.directory=""{userappdata}\Companyware\plugins-installed"" -jar ""{app}\Companyware.jar"""; WorkingDir: "{userappdata}\Companyware"
Name: "{autodesktop}\Companyware Desktop"; Filename: "{app}\runtime\bin\javaw.exe"; Parameters: "-Dcompanyware.persistence.embedded.path=""{userappdata}\Companyware\data\companyware"" -Dcompanyware.plugins.directory=""{userappdata}\Companyware\plugins-installed"" -jar ""{app}\Companyware.jar"""; WorkingDir: "{userappdata}\Companyware"; Tasks: desktopicon

[Run]
Filename: "{app}\runtime\bin\javaw.exe"; Parameters: "-Dcompanyware.persistence.embedded.path=""{userappdata}\Companyware\data\companyware"" -Dcompanyware.plugins.directory=""{userappdata}\Companyware\plugins-installed"" -jar ""{app}\Companyware.jar"""; WorkingDir: "{userappdata}\Companyware"; Description: "Companyware Desktop starten"; Flags: nowait postinstall skipifsilent
