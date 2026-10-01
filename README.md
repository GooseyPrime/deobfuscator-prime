# Java Deobfuscator

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-8%20--%2021%2B-orange.svg)](https://adoptium.net/)
[![ASM](https://img.shields.io/badge/ASM-9.7.1-brightgreen.svg)](https://ow2.org/)

An easy-to-install, modern, and powerful Java bytecode deobfuscation suite. This tool recovers clean bytecode from commercial and open-source Java obfuscators including **Zelix KlassMaster**, **Allatori**, **Stringer**, **Dash-O**, **Radon**, **DexGuard**, **Smoke**, and **SkidSuite**.

---

## What's New & Upgraded

* **Modern JDK & ASM 9.7.1 Compatibility**: Upgraded to OW2 ASM 9.7.1. Fully supports Java 8 bytecode up to modern Java (Java 9, 11, 17, 21+).
* **Native JMOD Runtime Support**: Full support for JDK 9+ `.jmod` modular runtimes (e.g., `java.base.jmod`), eliminating the old requirement of needing a legacy Java 8 `rt.jar`.
* **Integrated Graphical User Interface (GUI)**: Built-in Swing GUI with tabbed navigation, pipeline builder, live console logger, and 1-click presets.
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
  * **Flow Mode**: `NONE`, `LIGHT`, `NORMAL`
  * **Number Mode**: `NONE`, `LIGHT`, `NORMAL`
  * **String Pool Mode**: `NONE`, `CLEAN`
  * **Flags**: `Enable Indy`, `Enable Fast Indy`, `Decrypt Strings`, `Clean Trash Classes`

### 4. JVM Stack Memory (`-Xss128m`)
* **Why it is needed**: Advanced control flow deobfuscators traverse deeply nested syntax trees and flow graphs. On standard thread stack sizes (~1MB), Java will throw a `PreventableStackOverflowError`.
* **How to specify**: Always launch Java with `-Xss128m -Xmx2G`. This is automatically included in `run.sh` and `run.bat`.

---

## Graphical User Interface (GUI) Guide

Launch the GUI by running `./run.sh` or `java -Xss128m -jar target/deobfuscator-1.0.0.jar`.

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
1. **Select Target JAR**: On the **Target Files & Quick Start** tab, browse for your **Input JAR** and set the **Output JAR** destination.
2. **Auto-Detect Obfuscators**: Click **Run Obfuscator Detection**. The app scans the bytecode against detection rules and prompts to automatically configure the recommended transformer pipeline.
3. **Verify Runtime**: On the **Variables & Secrets** tab, ensure the Java Runtime Library path is filled in (click **Auto-Detect** if needed).
4. **Customize Pipeline**:
   - On the **Transformer Pipeline** tab, search through 70+ transformers.
   - Use **Add to Pipeline >>**, **Move Up**, **Move Down**, or **Remove** to customize the sequence.
   - Or pick a ready-made profile from the **Quick Preset** dropdown (Allatori, Zelix KlassMaster, Stringer, Dash-O, Radon, General Cleanup).
5. **Add Dependencies** *(if necessary)*: On the **Classpath & Dependencies** tab, add any external library JARs your application depends on.
6. **Execute**: Click **▶ Run Deobfuscator**. Switch to the **Live Console Log** tab to view progress and bytecode transformations in real time.

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
| **Stringer** | String hiding, invokedynamic decryption, hide-access | `stringer.v3.StringEncryptionTransformer`<br>`stringer.v3.InvokedynamicTransformer`<br>`stringer.v3.HideAccessObfuscationTransformer` |
| **Dash-O** | String encryption, flow obfuscation | `dasho.StringEncryptionTransformer`<br>`dasho.FlowObfuscationTransformer` |
| **Radon** | Flow, numbers, string pool, indy, trash classes | `radon.RadonTransformer`<br>`radon.RadonTransformerV2` |
| **Smoke** | Number decryption, string obfuscation | `smoke.NumberObfuscationTransformer`<br>`smoke.StringEncryptionTransformer` |
| **SkidSuite** | String encryption | `skidsuite.StringEncryptionTransformer` |
| **General / Peephole** | Dead code, constant folding, peephole jumps | `general.peephole.PeepholeOptimizer`<br>`general.peephole.DeadCodeRemover`<br>`general.peephole.ConstantFolder` |
| **Normalizers** | Source file, package, class, method, field naming | `normalizer.SourceFileNormalizer`<br>`normalizer.PackageNormalizer`<br>`normalizer.ClassNormalizer` |

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

