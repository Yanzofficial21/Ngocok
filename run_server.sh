#!/bin/bash

echo "🚀 Starting Server..."
python3 api.py &
echo "✅ Server running on port 5000"

echo "🤖 Starting Telegram Bot..."
python3 bot.py &
echo "✅ Bot running"

wait