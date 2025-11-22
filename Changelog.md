# Changelog 1.1:

- Added Themes: Dark, Light, Midnight and Ocean.
- Added a new button to the settings screen to access themes.
- New themes screen with previews of the message and contact screen, so you can see the themes in real time.
- Ability to change message bubble colors to different presets.

## Technical Changes:

- Removed all forms of DrawTextWithShadow - since it looked bad with black text.
- Removed all instances of Bold text - since it also looked bad with black text.
- The config file is now stored like this:
```json
{
  "allowServerLogging": true,
  "playNotificationSound": true,
  "showNotifications": true,
  "theme": "OCEAN",
  "sentBubbleColor": -16745729,
  "receivedBubbleColor": -12961220,
  "notificationDuration": 5000,
  "maxNotifications": 3
}
```


# Bug Fixes:

- Fixed some rendering issues with the back button on the message screen.
- Online players are now checked every frame, to make sure the player list is accurate - this comes from a bug where sometimes it wouldn't show the correct online players. 

# Future plans: 

- Clean up the code, remove any unnecessary things.
- Add a friends list / favourites list




