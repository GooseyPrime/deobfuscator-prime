# Java Deobfuscator

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%20--%2021%2B-orange.svg)](https://adoptium.net/)
[![ASM](https://img.shields.io/badge/ASM-9.7.1-brightgreen.svg)](https://ow2.org/)

An easy-to-install, modern, and powerful Java bytecode deobfuscation suite, with a universal front door for other file types. The Java engine recovers clean bytecode from commercial and open-source Java obfuscators including **Zelix KlassMaster**, **Allatori**, **Stringer**, **Dash-O**, **Radon**, **DexGuard**, **Smoke**, and **SkidSuite**. JavaScript, .NET, and Android files are handed to optional external tools when those tools are installed. They are not bundled, and a file is never described as deobfuscated unless a tool actually ran and changed the output.

---

## What's New & Upgraded

* **Modern JDK & ASM 9.7.1 Compatibility**: Upgraded to OW2 ASM 9.7.1. Fully supports Java 8 bytecode up to modern Java (Java 9, 11, 17, 21+).
* **Native JMOD Runtime Support**: Full support for JDK 9+ `.jmod` modular runtimes (e.g., `java.base.jmod`), eliminating the old requirement of needing a legacy Java 8 `rt.jar`.
* **Universal front door**: Drop or browse to a file. Magic bytes are checked first, then the extension. Java, JavaScript, .NET, Android, and unknown files each open on their own tab.
* **Integrated Graphical User Interface (GUI)**: The Java tab is the existing Swing UI (pipeline builder, live console, presets, result bytecode view). Other tabs call optional external tools on a background thread.
* **Auto-Detection Engine**: Automatically scans input JARs against bytecode signature rules and recommends the exact transformer pipeline.
* **Dedicated Variables & Secrets Manager**: UI entry fields with one-click auto-detection for Java runtime standard libraries, remapping dictionaries, and obfuscator flags.
* **Turnkey Launch Scripts**: Pre-configured `run.sh` (Linux/macOS) and `run.bat` (Windows) scripts with required JVM stack (`-Xss128m`) and memory options.
* **Zero Dead Dependencies**: All external build dependencies (`javavm`, `CAFED00D`) are vendored locally in `lib/repository/`, ensuring offline, reliable builds without failed remote repositories.

---

## Quick Start & Installation

### Prerequisites
* **Java Development Kit (JDK)**: Java 8 or higher (JDK 17 or 21 recommended).
* **Apache Maven**: Version 3.6 or higher.

To verify your environment:
```bash
java -version
mvn -version
```

### 1. Build from Source
Clone the repository and build the self-contained executable JAR:
```bash
mvn clean package -DskipTests
```
The shaded executable JAR will be generated at:
```
target/deobfuscator-1.0.0.jar
```

### 2. Launch the Application

#### Option A: Easy Launchers (Recommended)
The launcher scripts automatically verify Java, build the project if needed, and apply the critical `-Xss128m` JVM stack size:
* **Linux / macOS**:
  ```bash
  ./run.sh
  ```
* **Windows**:
  ```cmd
  run.bat
  ```

#### Option B: Graphical User Interface (GUI)
Launch the GUI directly via Java:
```bash
java -Xss128m -Xmx2G -jar target/deobfuscator-1.0.0.jar
```
*(or specify `--gui` explicitly)*

#### Option C: Command Line Interface (CLI)
Run headless deobfuscation using a YAML configuration file:
```bash
java -Xss128m -Xmx2G -jar target/deobfuscator-1.0.0.jar --config config.yaml
```

---

## Variables & Secrets Guide

Java Deobfuscator requires specific runtime files and parameters depending on your target application. All of these have dedicated input fields in the user interface.

| Variable / Parameter | Required? | Where to Get It | GUI Location |
| :--- | :--- | :--- | :--- |
| **Java Runtime Standard Library (`-path`)** | **Yes** (almost always) | From your local JDK installation (`rt.jar` for Java 8, or `jmods` folder / `java.base.jmod` for Java 9+). Can be auto-detected! | **Variables & Secrets** Tab &rarr; Section 1 |
| **Remapping / Mapping File** | Optional | SRG/MCP/Yarn mapping files for Minecraft, or custom dictionary text files for name remapping. | **Variables & Secrets** Tab &rarr; Section 2 |
| **Radon Flow/Number/Indy Modes** | Optional (for Radon) | Parameter values depending on the Radon obfuscator profile used on the target. | **Variables & Secrets** Tab &rarr; Section 3 |
| **Third-Party Libraries (`-libraries`)** | Optional (if referenced) | External JARs imported by the target application (e.g. Guava, Netty, Bukkit). | **Classpath & Dependencies** Tab |
| **Ignored Classes** | Optional | Class name prefixes or patterns to skip during transformation. | **Classpath & Dependencies** Tab |

### 1. Java Runtime Standard Library (`rt.jar` / `jmods`)
* **Why it is needed**: The deobfuscator needs to resolve class inheritance hierarchies and compute stack frames. If missing, it will report `NoClassInPathException`.
* **How to get it**:
  * **Java 8**: Found in `<JAVA_HOME>/jre/lib/rt.jar` (or `/usr/lib/jvm/java-8-openjdk/jre/lib/rt.jar` on Linux, `C:\Program Files\Java\jdk1.8.0_*\jre\lib\rt.jar` on Windows).
  * **Java 9, 11, 17, 21+**: Found in `<JAVA_HOME>/jmods` (or point directly to `<JAVA_HOME>/jmods/java.base.jmod`).
  * If you do not have a JDK installed, download one from [Eclipse Adoptium](https://adoptium.net/).
* **UI Entry**:
  1. Open the **Variables & Secrets** tab in the GUI.
  2. Click **⚡ Auto-Detect Runtime**: the app searches your active JVM and common installation paths automatically.
  3. Or click **Browse...** to select your `rt.jar` or `jmods/` folder manually.

### 2. Remapping & Mapping Files
* **Why it is needed**: Used by **Normalizer** transformers (`ClassNormalizer`, `MethodNormalizer`, `FieldNormalizer`) and Minecraft remappers to rename obfuscated identifiers (like `a`, `b`, `c`) into readable names.
* **How to get it**:
  * For standard remapping: Create a plain text dictionary file with one replacement name per line.
  * For Minecraft mods: Obtain MCP or SRG mapping files from [ParchmentMC](https://linkie.parchmentmc.org/) or the Forge Mappings repository.
  * ProGuard `.map` files generated during builds.
* **UI Entry**:
  1. Go to the **Variables & Secrets** tab.
  2. Under **2. Remapping / Mapping File**, click **Browse...** to select your mapping file.

### 3. Radon & Obfuscator Parameters
* **Why it is needed**: Radon obfuscator utilizes dynamic invokedynamic decryption, number encryption, and branch pool obfuscation.
* **Settings available in GUI**:
  * **Flow Mode**: `NONE`, `LIGHT`, `NORMAL_OR_HEAVY`, `COMBINED`
  * **Number Mode**: `NONE`, `LEGACY`, `NEW`
  * **String Pool Mode**: `NONE`, `LEGACY`, `NEW`
  * **Flags**: `Enable Indy`, `Enable Fast Indy`, `Decrypt Strings`, `Clean Trash Classes`

### 4. JVM Stack Memory (`-Xss128m`)
* **Why it is needed**: Advanced control flow deobfuscators traverse deeply nested syntax trees and flow graphs. On standard thread stack sizes (~1MB), Java will throw a `PreventableStackOverflowError`.
* **How to specify**: Always launch Java with `-Xss128m -Xmx2G`. This is automatically included in `run.sh` and `run.bat`.

---

## Universal front door

Launch the GUI with `./run.sh`, `run.bat`, or:

```bash
java -Xss128m -Xmx2G -jar target/deobfuscator-1.0.0.jar
```

The first tab is a drop target plus a Browse button. Detection order:

1. Magic bytes (`CA FE BA BE` class, `dex\n` DEX, `MZ` plus a CLR data directory, ZIP containers).
2. Filename extension, only when the bytes are ambiguous or have no recognized magic.

| Detected type | How it is recognized | What the tab does |
| :--- | :--- | :--- |
| Java | Class magic, or a ZIP/JAR/WAR (`.jar`, `.class`, `.war`) | Existing Java deobfuscator. A lone `.class` is packed into a new jar under `out/` first. |
| JavaScript | `.js` or `.mjs`, and no stronger magic | `webcrack` (preferred) or `synchrony`, via a local binary or `npx` |
| .NET | PE file whose CLR runtime directory is present | `de4dot` |
| Android | DEX magic, or a ZIP with `AndroidManifest.xml` / `classes.dex`, or `.apk` | `jadx` (preferred) or `apktool` |
| Unknown | Anything else, including a PE without a CLR header | Reported on the Unknown tab. Not processed. |

Outputs are written under `./out` and are never the input path. If a tool is missing, its tab says which command is missing and how to install it, and Run stays disabled. `npx --yes webcrack` is offered as a manual Run because it may download a package; dropping a `.js` file does not start that download. A finished run that does not change the input is reported as not deobfuscated.

Each non-Java tab has a path field. Paths are remembered in `~/.deobfuscator-prime/tool-paths.properties`. Detect also searches `PATH`. Work runs off the Swing thread. Progress and errors go to the log pane. Text results are shown beside the original; Java shows a bytecode dump of the input and the output jar.

### Optional external tools

These are not required to build or to deobfuscate Java.

| Tool | Type | Install |
| :--- | :--- | :--- |
| webcrack | JavaScript | Install Node.js, then `npm install -g webcrack`. Command: `webcrack <file> -o <directory>` |
| synchrony | JavaScript | `npm install -g synchrony`. Command: `synchrony <file> -o <output.js>` |
| de4dot | .NET | https://github.com/de4dot/de4dot/releases . On Linux/macOS a `de4dot.exe` build also needs `mono`. Command: `de4dot <assembly> -o <output>` |
| jadx | Android | https://github.com/skylot/jadx/releases or `sudo apt install jadx`. Command: `jadx -d <directory> <apk-or-dex>` |
| apktool | Android | https://apktool.org/docs/install or `sudo apt install apktool`. Command: `apktool d -f -o <directory> <apk>` |

Made-up fixtures live in `samples/test-resources/` (a padded class jar, a tiny string-table script, a synthetic PE, a fake apk, and an unknown blob). They are not real protected programs.

## Graphical User Interface (GUI) Guide

Launch the GUI by running `./run.sh` or `java -Xss128m -jar target/deobfuscator-1.0.0.jar`. The window opens on the front page. The Java tab is the workflow below.

```
+--------------------------------------------------------------------------------+
|  Java Deobfuscator                                                             |
|  File   Config   Help                                                          |
+--------------------------------------------------------------------------------+
| [Target Files & Quick Start] [Variables & Secrets] [Classpath] [Pipeline] [Log]|
|                                                                                |
|  Input JAR:  [/path/to/obfuscated.jar              ] [ Browse... ]             |
|  Output JAR: [/path/to/deobfuscated.jar            ] [ Browse... ]             |
|                                                                                |
|  [ Run Obfuscator Detection ]                                                  |
|  Quick Preset: [ Allatori Full Deobfuscation      v ] [ Load Preset ]          |
|                                                                                |
|  [x] Verify Bytecode (CheckClassAdapter)    [ ] Patch ASM Method Size Limit    |
|  [ ] Smart Redo Loop                        [ ] Delete Useless Classes         |
+--------------------------------------------------------------------------------+
|  Status: Ready                                          [⏹ Cancel] [▶ Run]     |
+--------------------------------------------------------------------------------+
```

### Steps to Deobfuscate via GUI:
Dropping a jar, war, or class file on the front page opens this Java tab, sets the output under `out/`, and runs the pipeline. If the pipeline is empty, the peephole cleanup preset is applied. You can also drive the same screen by hand:

1. **Select Target JAR**: On the **Target Files** tab, browse for your **Input JAR**. The output path defaults to `out/<name>-deobfuscated.jar`.
2. **Auto-Detect Obfuscators**: Click **Run Obfuscator Detection**. The app scans the bytecode against detection rules and prompts to automatically configure the recommended transformer pipeline.
3. **Verify Runtime**: On the **Variables & Secrets** tab, ensure the Java Runtime Library path is filled in (click **Auto-Detect** if needed).
4. **Customize Pipeline**:
   - On the **Transformer Pipeline** tab, search through 70+ transformers.
   - Use **Add to Pipeline >>**, **Move Up**, **Move Down**, or **Remove** to customize the sequence.
   - Or pick a ready-made profile from the **Quick Preset** dropdown (Allatori, Zelix KlassMaster, Stringer, Dash-O, Radon, General Cleanup).
5. **Add Dependencies** *(if necessary)*: On the **Classpath & Dependencies** tab, add any external library JARs your application depends on.
6. **Execute**: Click **Run Deobfuscator**. The **Console Log** tab shows progress. The **Result** tab shows the status (detected type, obfuscator guess, transformers, whether the bytecode changed) and a side-by-side bytecode dump. A run that does not change the bytecode is not reported as deobfuscated.

---

## Command Line Interface (CLI) Guide

### Command Line Options
```
usage: java -jar deobfuscator.jar [options]
 -c,--config <file>   The YAML configuration file to use
 -g,--gui             Launch the Graphical User Interface (GUI)
 -h,--help            Print help and usage information
 -v,--version         Print version information
```

### 1. Step 1: Detect Obfuscators (CLI)
Create a file named `detect.yaml`:
```yaml
input: sample-obfuscated.jar
path:
  - "/usr/lib/jvm/java-17-openjdk/jmods"  # or /path/to/rt.jar for Java 8
detect: true
```

Run detection:
```bash
java -Xss128m -jar target/deobfuscator-1.0.0.jar --config detect.yaml
```

Output example:
```
[INFO] Loading classpath...
[INFO] Loading input...
[INFO] Detecting...
[INFO] Recommend: allatori.StringEncryptionTransformer
[INFO] Recommend: allatori.FlowObfuscationTransformer
```

### 2. Step 2: Run Deobfuscation (CLI)
Create a file named `config.yaml` with the recommended transformers:
```yaml
input: sample-obfuscated.jar
output: sample-deobfuscated.jar

# Java runtime path: rt.jar for Java 8, or jmods for Java 9+
path:
  - "/usr/lib/jvm/java-17-openjdk/jmods"

# External dependencies (optional)
libraries:
  - "lib/guava.jar"
  - "lib/gson.jar"

# Engine verification
verify: true

# Ordered transformer pipeline
transformers:
  - com.javadeobfuscator.deobfuscator.transformers.allatori.StringEncryptionTransformer
  - com.javadeobfuscator.deobfuscator.transformers.allatori.FlowObfuscationTransformer
  - com.javadeobfuscator.deobfuscator.transformers.general.peephole.PeepholeOptimizer
```

Execute deobfuscation:
```bash
java -Xss128m -Xmx2G -jar target/deobfuscator-1.0.0.jar --config config.yaml
```

---

## Supported Obfuscators & Transformers

| Obfuscator | Features Deobfuscated | Recommended Transformers |
| :--- | :--- | :--- |
| **Allatori** | String encryption, control flow, light flow | `allatori.StringEncryptionTransformer`<br>`allatori.FlowObfuscationTransformer` |
| **Zelix KlassMaster (ZKM)** | String decryption, flow obfuscation, reflection | `zelix.StringEncryptionTransformer`<br>`zelix.FlowObfuscationTransformer`<br>`zelix.ReflectionObfuscationTransformer` |
| **Stringer** | String hiding, invokedynamic decryption, hide-access | `stringer.v3.StringEncryptionTransformer`<br>`stringer.v3.InvokedynamicTransformer`<br>`stringer.v3.HideAccessTransformer` |
| **Dash-O** | String encryption, flow obfuscation | `dasho.StringEncryptionTransformer`<br>`dasho.FlowObfuscationTransformer` |
| **Radon** | Flow, numbers, string pool, indy, trash classes | `special.RadonTransformer`<br>`special.RadonTransformerV2` |
| **Smoke** | Number decryption, string obfuscation | `smoke.NumberObfuscationTransformer`<br>`smoke.StringEncryptionTransformer` |
| **SkidSuite** | String encryption | `skidsuite2.StringEncryptionTransformer` |
| **General / Peephole** | Dead code, constant folding, peephole jumps | `general.peephole.PeepholeOptimizer`<br>`general.peephole.DeadCodeRemover`<br>`general.peephole.ConstantFolder` |
| **Normalizers** | Source file, package, class, method, field naming | `normalizer.SourceFileClassNormalizer`<br>`normalizer.PackageNormalizer`<br>`normalizer.ClassNormalizer` |

---

## Troubleshooting & FAQs

#### Q: I get `NoClassInPathException: Could not locate a class file.`
**A**: Java Deobfuscator requires the Java standard library runtime and any target application dependencies to perform bytecode analysis:
1. On the **Variables & Secrets** tab, click **Auto-Detect** or browse to your `rt.jar` (Java 8) or `jmods/` folder (Java 9+).
2. If the missing class belongs to a library (e.g. `com/google/gson/Gson`), add that library JAR to the **Classpath & Dependencies** tab (or `libraries:` in YAML).

#### Q: I get `PreventableStackOverflowError: Try increasing your stack size using the -Xss flag.`
**A**: Complex obfuscators (like Zelix and Radon) produce heavily recursive control flow graphs. Add `-Xss128m` to your Java launch flags:
```bash
java -Xss128m -Xmx2G -jar target/deobfuscator-1.0.0.jar
```
Using `./run.sh` or `run.bat` automatically handles this for you.

#### Q: How do I run the GUI on a headless Linux server?
**A**: You can use X11 forwarding (`ssh -X`) or `xvfb` (virtual frame buffer):
```bash
sudo apt install -y xvfb
xvfb-run -a ./run.sh
```
Alternatively, run in headless CLI mode using `--config config.yaml`.

#### Q: Can I run this offline without internet access?
**A**: Yes! All required external libraries (`javavm`, `CAFED00D`) are vendored in `lib/repository`. The project builds and runs completely offline.

---

## License

Java Deobfuscator is licensed under the [Apache 2.0 License](LICENSE).
