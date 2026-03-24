from flask import Flask, request, jsonify
import sqlite3
import uuid
from datetime import datetime

app = Flask(__name__)

def get_db():
    conn = sqlite3.connect('controller.db')
    conn.row_factory = sqlite3.Row
    return conn

@app.route('/register_device', methods=['POST'])
def register_device():
    data = request.json
    username = data.get('username')
    password = data.get('password')
    device_name = data.get('device_name')
    
    conn = get_db()
    # Validasi user
    user = conn.execute("SELECT id FROM users WHERE username=? AND password=?", 
                        (username, password)).fetchone()
    
    if not user:
        return jsonify({'success': False, 'error': 'Invalid credentials'})
    
    device_id = str(uuid.uuid4())
    conn.execute("INSERT OR REPLACE INTO devices (device_id, device_name, user_id, status, last_seen) VALUES (?,?,?,?,?)",
                 (device_id, device_name, user[0], 'online', datetime.now().isoformat()))
    conn.commit()
    conn.close()
    
    return jsonify({'success': True, 'device_id': device_id})

@app.route('/get_commands', methods=['GET'])
def get_commands():
    device_id = request.args.get('device_id')
    conn = get_db()
    commands = conn.execute("SELECT id, command FROM commands WHERE device_id=? AND status='pending'",
                           (device_id,)).fetchall()
    
    # Update status ke 'sent'
    for cmd in commands:
        conn.execute("UPDATE commands SET status='sent' WHERE id=?", (cmd['id'],))
    conn.commit()
    conn.close()
    
    return jsonify([dict(cmd) for cmd in commands])

@app.route('/report_result', methods=['POST'])
def report_result():
    data = request.json
    device_id = data.get('device_id')
    command_id = data.get('command_id')
    result = data.get('result')
    
    conn = get_db()
    conn.execute("UPDATE commands SET status='executed', result=? WHERE id=?",
                 (result, command_id))
    conn.commit()
    conn.close()
    
    return jsonify({'success': True})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)