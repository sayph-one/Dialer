#!/bin/bash

# Monitor incoming calls and activity launches on physical device
# Usage: ./monitor_calls.sh [device_serial]

DEVICE=$1

if [ -z "$DEVICE" ]; then
    echo "Usage: ./monitor_calls.sh <device_serial>"
    echo ""
    echo "Available devices:"
    adb devices
    exit 1
fi

echo "================================"
echo "Monitoring calls on device: $DEVICE"
echo "================================"
echo ""
echo "Watching for:"
echo "- Activity starts (to see which app handles calls)"
echo "- Phone app activity"
echo "- Telecom service events"
echo "- Call intents"
echo ""
echo "Make a call to this device now..."
echo ""

# Clear logcat first
adb -s "$DEVICE" logcat -c

# Monitor logcat for:
# - ActivityManager: to see which activities are launched
# - Telecom: to see call handling
# - Phone app logs
adb -s "$DEVICE" logcat \
    ActivityManager:I \
    ActivityTaskManager:I \
    Telecom:D \
    TelecomFramework:D \
    InCallController:D \
    MainActivity:D \
    CallActivity:D \
    SimpleCallScreeningService:D \
    *:S
