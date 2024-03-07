# Eclair Alarm Bot Plugin

Simplest possible [Eclair](https://github.com/ACINQ/eclair) plugin. It sends Eclair notifications to a Telegram bot. The latest supported version of Eclair is 0.9.0.

It can be upgraded with new events or even important node statistics.

## How to build

First you need to build its dependencies

```bash
git clone https://github.com/ACINQ/eclair.git

cd eclair/

git checkout v0.10.0

mvn install -DskipTests=true
```

Then build the plugin
```bash
git clone https://github.com/evd0kim/eclair-alarmbot-plugin.git

cd eclair-alarmbot-plugin/

mvn install
```

The `mvn` command will put the plugin's JAR file into `target` directory. 

## Hot to run

Simply add the JAR file name to the Eclair node command line:

```bash
<PATH_TO_YOUR_ECLAIR_INSTALLATION>/eclair-node.sh target/eclair-alarmbot_2.13-0.10.0.jar
```

## Configuration

It needs config file in `<ECLAIR_DATA_DIR>/plugin-resources/alarmbot/alarmbot.conf`

```
config {
    botApiKey = "PUT_YOUR_BOT_KEY_HERE"
    chatId = "PUT_ID_OF_YOUR_PERSONAL_CHAT"
    hedgeServiceUri = "OPTIONAL_URL_TO_YOUR_HEDGE_SERVICE"
    useProxy = "IF_TRUE_USE_ECLAIR_PROXY"
}
```

## How to get Telegram `chatId`?

1. Start a new bot
2. Use `curl` for getting bot updates from Telegram
```
curl https://api.telegram.org/bot<PUT_YOUR_BOT_KEY_HERE>/getUpdates
```
3. You should get response from Telegram server where you can find your 
   `chatId`. If it didn't happen, try to write something in the Bot chat
   and repeat step 2.