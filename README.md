Simplest possible Eclair plugin.

Can be upgraded with new events or even important node statistics.

Needs config file in `<ECLAIR_ROOT>/plugin-resources/alarmbot/alarmbot.conf`

```
config {
    botApiKey = "PUT_YOUR_BOT_KEY_HERE"
    chatId = "PUT_ID_OF_YOUR_PERSONAL_CHAT"
}
```

## How to get `chatId`?

1. Start new bot
2. Use `curl` for getting bot updates from Telegram
```
curl https://api.telegram.org/bot<PUT_YOUR_BOT_KEY_HERE>/getUpdates
```
3. You should get response from Telegram server where you can find your 
   `chatId`. If it didn't happen, try to write something in the Bot chat
   and repeat 2.