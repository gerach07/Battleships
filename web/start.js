#!/usr/bin/env node

const { exec, spawn } = require('child_process');
const { promisify } = require('util');
const execAsync = promisify(exec);
const os = require('os');

const PORTS = {
  client: 3000,
  server: 3001
};

const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  white: '\x1b[37m'
};

function log(message, color = colors.white) {
  console.log(`${color}${message}${colors.reset}`);
}

function banner() {
  log('\n' + '='.repeat(60), colors.cyan);
  log('⚔️  BATTLESHIPS DEVELOPMENT SERVER', colors.bright + colors.cyan);
  log('='.repeat(60) + '\n', colors.cyan);
}

async function checkPort(port) {
  try {
    if (os.platform() === 'win32') {
      const result = await execAsync(`netstat -ano | findstr :${port} | findstr LISTENING`);
      const pids = result.stdout.trim().split('\n')
        .map(l => l.trim().split(/\s+/).pop())
        .filter(Boolean);
      return pids.length > 0 ? pids : null;
    } else {
      const { stdout } = await execAsync(`lsof -ti:${port} 2>/dev/null || echo ""`);
      const pids = stdout.trim().split('\n').filter(Boolean);
      return pids.length > 0 ? pids : null;
    }
  } catch (error) {
    return null;
  }
}

async function killProcesses(pids, portName) {
  try {
    if (os.platform() === 'win32') {
      for (const pid of pids) {
        await execAsync(`taskkill /F /PID ${pid}`).catch(() => {});
      }
    } else {
      await execAsync(`kill -9 ${pids.join(' ')}`);
    }
    log(`✓ Cleaned up old ${portName} processes`, colors.green);
    return true;
  } catch (error) {
    log(`✗ Failed to kill ${portName} processes: ${error.message}`, colors.red);
    return false;
  }
}

async function checkAndCleanPorts() {
  const issues = [];
  
  for (const [name, port] of Object.entries(PORTS)) {
    const pids = await checkPort(port);
    if (pids) {
      issues.push({ name, port, pids });
    }
  }

  if (issues.length === 0) {
    log('✓ All ports are available', colors.green);
    return true;
  }

  log(`⚠️  Found ${issues.length} port conflict${issues.length > 1 ? 's' : ''}:`, colors.yellow);
  issues.forEach(({ name, port, pids }) => {
    log(`   Port ${port} (${name}): ${pids.length} process${pids.length > 1 ? 'es' : ''} running`, colors.yellow);
  });

  log('\n🔧 Auto-cleaning old processes...', colors.cyan);
  
  for (const { name, port, pids } of issues) {
    await killProcesses(pids, name);
  }

  // Verify ports are now free
  await new Promise(resolve => setTimeout(resolve, 500));
  
  for (const { port } of issues) {
    const stillInUse = await checkPort(port);
    if (stillInUse) {
      log(`\n✗ Port ${port} is still in use. Please close the application manually.`, colors.red);
      log(`  Run: lsof -ti:${port} | xargs kill -9`, colors.yellow);
      return false;
    }
  }

  log('✓ All ports cleaned successfully\n', colors.green);
  return true;
}

async function startServers() {
  banner();
  
  log('🔍 Checking ports...', colors.cyan);
  const portsReady = await checkAndCleanPorts();
  
  if (!portsReady) {
    log('\n❌ Cannot start servers due to port conflicts', colors.red);
    log('Please resolve the issues above and try again.\n', colors.yellow);
    process.exit(1);
  }

  log('🚀 Starting development servers...\n', colors.cyan);
  
  const dev = spawn('npm', ['run', 'dev:internal'], {
    stdio: 'inherit',
    shell: true
  });

  dev.on('error', (error) => {
    log(`\n❌ Failed to start: ${error.message}`, colors.red);
    process.exit(1);
  });

  dev.on('exit', (code) => {
    if (code !== 0 && code !== 130) { // 130 is Ctrl+C
      log(`\n⚠️  Server exited with code ${code}`, colors.yellow);
    }
    process.exit(code);
  });

  // Handle graceful shutdown
  process.on('SIGINT', () => {
    log('\n\n👋 Shutting down gracefully...', colors.cyan);
    dev.kill('SIGINT');
  });

  process.on('SIGTERM', () => {
    dev.kill('SIGTERM');
  });
}

startServers().catch((error) => {
  log(`\n❌ Unexpected error: ${error.message}`, colors.red);
  console.error(error);
  process.exit(1);
});
