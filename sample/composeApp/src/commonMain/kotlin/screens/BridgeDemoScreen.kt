package screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aryapreetam.cmpwebview.WebView
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeDemoScreen(onBack: () -> Unit = {}) {
  var selectedDemo by remember { mutableStateOf<DemoType?>(null) }
  var lastMessage by remember { mutableStateOf("No messages yet") }
  var messageCount by remember { mutableStateOf(0) }
  var webViewKey by remember { mutableStateOf(0) }
  var bridgeReady by remember { mutableStateOf(false) }

  // Reset state when demo changes
  LaunchedEffect(selectedDemo) {
    if (selectedDemo != null) {
      bridgeReady = false
      lastMessage = "No messages yet"
      messageCount = 0
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            when (selectedDemo) {
              DemoType.BRIDGE_TEST -> "Basic Bridge Test"
              DemoType.FORM_DEMO -> "Form Submission Demo"
              DemoType.INTERACTIVE -> "Interactive Demo"
              null -> "Bridge Communication Demos"
            }
          )
        },
        navigationIcon = {
          IconButton(
            onClick = {
              if (selectedDemo != null) {
                // Go back to demo selection
                selectedDemo = null
                lastMessage = "No messages yet"
                messageCount = 0
                bridgeReady = false
              } else {
                // Go back to home screen
                onBack()
              }
            }
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      if (selectedDemo == null) {
        // Demo selection screen
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
          Text(
            text = "Select a demo to test bridge communication:",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
          )

          DemoCard(
            title = "Basic Bridge Test",
            description = "Test simple messages, JSON, counter, and stress test with 100 rapid messages",
            onClick = {
              selectedDemo = DemoType.BRIDGE_TEST
              webViewKey++
            }
          )

          DemoCard(
            title = "Form Submission Demo",
            description = "Submit a form and receive structured JSON data",
            onClick = {
              selectedDemo = DemoType.FORM_DEMO
              webViewKey++
            }
          )

          DemoCard(
            title = "Interactive Demo",
            description = "Real-time interactions: counter, color picker, slider",
            onClick = {
              selectedDemo = DemoType.INTERACTIVE
              webViewKey++
            }
          )
        }
      } else {
        // WebView with selected demo
        Column(modifier = Modifier.fillMaxSize()) {
          // Header with bridge status and message display
          Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              // Bridge status row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = if (bridgeReady) "Bridge: Ready" else "Bridge: Waiting…",
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = "Messages: $messageCount",
                  style = MaterialTheme.typography.titleMedium
                )
              }

              Text(
                text = "Last: $lastMessage",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }

          // WebView with unique key to force recreation
          key(webViewKey) {
            WebView(
              htmlContent = getHtmlContent(selectedDemo!!),
              modifier = Modifier.fillMaxSize(),
              onScriptResult = { message ->
                if (!bridgeReady) bridgeReady = true
                lastMessage = message.take(100)
                messageCount++
              },
              onLoadStarted = {
                // Page started loading
              },
              onLoadFinished = {
                bridgeReady = true
              },
              onLoadError = { error ->
                lastMessage = "Error: $error"
                bridgeReady = false
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun DemoCard(
  title: String,
  description: String,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
      )
      Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Launch Demo")
      }
    }
  }
}

enum class DemoType {
  BRIDGE_TEST,
  FORM_DEMO,
  INTERACTIVE
}

fun getHtmlContent(demoType: DemoType): String {
  // Using inline HTML for now - works on all platforms
  return when (demoType) {
    DemoType.BRIDGE_TEST -> getBridgeTestHtml()
    DemoType.FORM_DEMO -> getFormDemoHtml()
    DemoType.INTERACTIVE -> getInteractiveDemoHtml()
  }
}

// Bridge Test HTML
fun getBridgeTestHtml(): String = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bridge Test</title>
    <style>
        body { font-family: sans-serif; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
        .container { background: rgba(255, 255, 255, 0.1); border-radius: 12px; padding: 20px; }
        button { background: white; color: #667eea; border: none; padding: 12px 24px; border-radius: 8px; 
                 font-size: 16px; cursor: pointer; margin: 8px 0; width: 100%; }
        .status { margin-top: 20px; padding: 12px; background: rgba(255, 255, 255, 0.2); border-radius: 8px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🌉 Bridge Test</h1>
        <div id="status" class="status">Waiting for bridge...</div>
        <button onclick="sendSimpleMessage()">Send Simple Message</button>
        <button onclick="sendJsonMessage()">Send JSON Message</button>
        <button onclick="sendRapidMessages()">Send 100 Messages</button>
    </div>
    <script>
        document.addEventListener('ComposeWebViewBridgeReady', function() {
            document.getElementById('status').textContent = '✅ Bridge is ready!';
        });
        
        function sendSimpleMessage() {
            if (window.ComposeWebViewBridge) {
                window.ComposeWebViewBridge.postMessage('Hello from JavaScript!');
                document.getElementById('status').textContent = '✅ Simple message sent';
            }
        }
        
        function sendJsonMessage() {
            if (window.ComposeWebViewBridge) {
                const msg = JSON.stringify({ type: 'test', data: 'JSON message', timestamp: Date.now() });
                window.ComposeWebViewBridge.postMessage(msg);
                document.getElementById('status').textContent = '✅ JSON message sent';
            }
        }
        
        function sendRapidMessages() {
            if (window.ComposeWebViewBridge) {
                for (let i = 1; i <= 100; i++) {
                    window.ComposeWebViewBridge.postMessage(JSON.stringify({ type: 'stress_test', messageNumber: i }));
                }
                document.getElementById('status').textContent = '✅ 100 messages sent!';
            }
        }
        
        if (window.ComposeWebViewBridge) {
            document.getElementById('status').textContent = '✅ Bridge is ready!';
        }
    </script>
</body>
</html>
""".trimIndent()

fun getFormDemoHtml(): String = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Form Demo</title>
    <style>
        body { font-family: sans-serif; padding: 20px; background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; }
        .container { background: rgba(255, 255, 255, 0.1); border-radius: 12px; padding: 20px; }
        input, select { width: 100%; padding: 12px; margin: 8px 0; border-radius: 8px; border: none; box-sizing: border-box; }
        button { background: white; color: #f5576c; border: none; padding: 12px 24px; border-radius: 8px; 
                 font-size: 16px; cursor: pointer; width: 100%; margin-top: 16px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>📝 Form Demo</h1>
        <form onsubmit="handleSubmit(event)">
            <input type="text" id="name" placeholder="Name" required>
            <input type="email" id="email" placeholder="Email" required>
            <select id="country">
                <option value="US">United States</option>
                <option value="UK">United Kingdom</option>
                <option value="IN">India</option>
            </select>
            <button type="submit">Submit Form</button>
        </form>
    </div>
    <script>
        function handleSubmit(event) {
            event.preventDefault();
            if (window.ComposeWebViewBridge) {
                const data = JSON.stringify({
                    type: 'form_submission',
                    name: document.getElementById('name').value,
                    email: document.getElementById('email').value,
                    country: document.getElementById('country').value
                });
                window.ComposeWebViewBridge.postMessage(data);
                console.log('Form submitted!');
                event.target.reset();
            }
        }
    </script>
</body>
</html>
""".trimIndent()

fun getInteractiveDemoHtml(): String = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Interactive Demo</title>
    <style>
        body { font-family: sans-serif; padding: 20px; background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; }
        .container { background: rgba(255, 255, 255, 0.1); border-radius: 12px; padding: 20px; }
        button { background: white; color: #4facfe; border: none; padding: 10px 20px; border-radius: 8px; 
                 font-size: 14px; cursor: pointer; margin: 4px; }
        .counter { font-size: 48px; text-align: center; margin: 20px 0; font-weight: bold; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🎮 Interactive Demo</h1>
        <div class="counter" id="counter">0</div>
        <button onclick="increment()">➕ Increment</button>
        <button onclick="decrement()">➖ Decrement</button>
        <button onclick="reset()">🔄 Reset</button>
    </div>
    <script>
        let counter = 0;
        function sendMessage(type, data) {
            if (window.ComposeWebViewBridge) {
                window.ComposeWebViewBridge.postMessage(JSON.stringify({ type: type, data: data }));
            }
        }
        function increment() {
            counter++;
            document.getElementById('counter').textContent = counter;
            sendMessage('counter_change', { value: counter, action: 'increment' });
        }
        function decrement() {
            counter--;
            document.getElementById('counter').textContent = counter;
            sendMessage('counter_change', { value: counter, action: 'decrement' });
        }
        function reset() {
            counter = 0;
            document.getElementById('counter').textContent = counter;
            sendMessage('counter_change', { value: counter, action: 'reset' });
        }
    </script>
</body>
</html>
""".trimIndent()
