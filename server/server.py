import asyncio
import websockets
import pyperclip
import json as json
import secrets
import qrcode
from urllib.parse import urlparse, parse_qs
import socket
import os 
import logging as logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')


connected_clients = set()
last_clipboard = pyperclip.paste()
generated_token = secrets.token_urlsafe(16)
print(f"Secret key for clipboard sync: {generated_token}")
last_recived_from_client = None

used_token= set()

async def clipboardMonitor():
    global last_clipboard
    global last_recived_from_client
    print("Monitoring clipboard...")
    while True:
        # Simulate clipboard monitoring
        current_clipboard = pyperclip.paste()  
        if current_clipboard != last_clipboard and current_clipboard != last_recived_from_client:
            last_clipboard = current_clipboard
            print(f"Clipboard changed: {current_clipboard}")
            # Notify all connected clients about the clipboard change
            if connected_clients:
                try :
                    await asyncio.gather(*(client.send(current_clipboard) for client in connected_clients))
                    print(f"Sent message to {len(connected_clients)} clients: {current_clipboard}")
                except Exception as e:
                    print(f"Error sending message to clients: {e}")
                    
        await asyncio.sleep(1)  # Replace with actual clipboard monitoring logic

async def handler(websocket):
    global last_recived_from_client
    
    path = websocket.request.path
    parsed_url = urlparse(path)
    query_params = parse_qs(parsed_url.query)
    request_token = query_params.get('token', [None])[0]  
    
    if generated_token == request_token:
        connected_clients.add(websocket)
    else:
        print(f"Invalid token: {request_token}. Connection refused.")
        await websocket.close(code=4001, reason="Invalid token")
        return
    
    logging.info(f"New client connected: {websocket.remote_address}")
    try:
        async for message in websocket:
            try:
                logging.info(f"Received message from {websocket.remote_address}: {message}")
                last_recived_from_client = message
                pyperclip.copy(message)
            except Exception as e:
                print(f"Error copying to clipboard: {e}")
    
    except Exception as e:
        print(f"Error: {e}")
    finally:
        connected_clients.remove(websocket)
        
def get_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))  # Connect to a public DNS server
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception as e:
        print(f"Error getting local IP address: {e}")
        return 
            
def open_qr_code(filename):
    """Open the QR code image in the default image viewer"""
    try:
        if os.name == 'nt':  # Windows
            os.startfile(filename)
        elif os.name == 'posix':  # macOS and Linux
            if os.uname().sysname == 'Darwin':  # macOS
                os.system(f"open {filename}")
            else:  # Linux
                os.system(f"xdg-open {filename}")
        print(f"QR code opened: {filename}")
        
    except Exception as e:
        print(f"Could not open QR code automatically: {e}")
        print(f"Please manually open: {filename}")


ip = get_ip()

def generate_qr_code():
    """Generate a QR code for the WebSocket server URL"""
    try:
        global generated_token
       
        port = 8765

        url = f"ws://{ip}:{port}/?token={generated_token}"
        qr = qrcode.QRCode(version=1, box_size=10, border=5)
        qr.add_data(url)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white")
        img.save("clipboard_sync_qr.png")
        open_qr_code("clipboard_sync_qr.png")
        print(f"QR code generated and saved as 'clipboard_sync_qr.png'. Scan it to connect.")
    
    except Exception as e:
        print(f"Error generating QR code: {e}")

async def startServer():
    # ip = "192.168.1.10"  # Replace with your actual local IP
    
    port = 8765
    async with websockets.serve(handler, "0.0.0.0", port):
        print(f"WebSocket server started at ws://{ip}:{port}")
        # await asyncio.gather(asyncio.Future())
        await asyncio.gather(asyncio.Future(), clipboardMonitor())

  # Keep the server running forever
  
async def main():
    generate_qr_code()
    await startServer()
  

asyncio.run(main())
# generate_qr_code()
