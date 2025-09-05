### VEGAS ADDON - SIMPLE EXAMPLE
This project shows how to add a new game mode to the Vegas plugin. This game mode is already in the original Vegas plugin and this addon is only a training one.

You can skip reading the information below and use the code itself to study!

#### Add dependencies:

VegasAPI implements the methods themselves that are needed to implement the game mode into the plugin.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Shimado</groupId>
        <artifactId>VegasAPI</artifactId>
        <version>v1.0.11</version>
    </dependency>
</dependencies>
```

BasicUtils is my local library that has many methods that make it easier to work with inventories, items, etc. This is not necessary! You can implement this with your own methods. To install this library, do the following:

```xml
<dependencies>
    <dependency>
        <groupId>com.shimado.basicutils</groupId>
        <artifactId>BasicUtils</artifactId>
        <version>1.2.13</version>
    </dependency>
</dependencies>
```

```bash
mvn install:install-file -Dfile=lib/BasicUtils.jar -DgroupId=com.shimado.basicutils -DartifactId=BasicUtils -Dversion=1.2.13 -Dpackaging=jar
```

After adding this dependency it will be in memory. You can delete the **/lib** folder with **BasicUtils.jar** itself!

Dependencies are set up! Let's move on!

#### Setting up plugin.yml

```yaml
name: VegasAddon
version: '@revision@'
main: org.shimado.addon.VegasAddon
api-version: 1.13
depend: [ Vegas ]  # <= MUST TO BE
folia-supported: true  # <= If you want to use folia
```

#### [OPTIONAL] Setting up config.yml and en.yml

Data can be transferred to the CasinoGameMode super class using methods without a config or message config. Directly through the code. But a more correct option would be to move the general mode data to configs, so that you can quickly change, for example, the inventory header, coefficients, etc.

Below are examples of basic config, the fields of which the Vegas plugin itself finds and implements into your mode. For example, the name of the mode, the number of inventory lines, etc.

**[OPTIONAL]** fields - can be removed

**[REQUIRED]** fields

```yaml
Example-settings:                   # [Required] Any key you want without spaces.
  mode-name: 'Drums'                # [Required] The name that will be used. Should not be long, no spaces, only letters and numbers.
  permission: 'casino.drums'        # [Optional] The permission are needed to use the mode.
  gui-size: 6                       # [Optional] GUI size from 1 to 6. By default - 6.
  icon:                             # [Optional] It is necessary to output to the GUI. If there is no icon, then you can open it with the command.
    material: 'YELLOW_SHULKER_BOX'  # [Required] Material or head URL of the icon.
    custom-model-data: 0            # [Optional] Custom model data for the material.
    slot:                           # [Optional] Defines slots.
      all-games: 12                 # [Optional] Slot in the general game menu.
      one-game: 22                  # [Optional] Slot in the menu for one game.
  jackpot:                          # [Optional] Defines jackpot event.
    chance: 0.05                    # [Optional] Chance to hit the jackpot if you win.
    boost: 1.0                      # [Optional] Jackpot total bet booster. Jackpot Amount * Booster.
    charged-percentage: 10.0        # [Optional] The percentage of the bet will go towards the jackpot amount.
    message-to:                     # [Optional] Defines messages to this event. Same for another events!
      player: true                  # [Optional] - A message for you.
      all: true                     # [Optional] - A message for all players except the player.
      discord: true                 # [Optional] - A message to discord, if you have DiscordSRV plugin.
    commands:                       # [Optional] Defines commands to this event. Same for another events!
      execute-only-commands: false  # [Optional] Will only the commands be executed, without processing the issuance of the prize itself?
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %prize%, %bet_price_rounded%, %prize_rounded%
      commands: []                  # [Optional] Commands list to dispatch. For example: eco give %player_name% 100
    http:                           # [Optional] Defines http/https requests to this event. Same for another events!
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %prize%, %bet_price_rounded%, %prize_rounded%
      url: ''                       # [Optional] A URL link to perform a request on. For example: https://www.youtube.com
      method: 'POST'                # [Optional] Methods: GET, POST, PUT, DELETE
      headers:                      # [Optional] Example below
        Content-Type: "application/json"
    music: 'jackpot'                # [Optional] Defines music to this event. You need to enter here music ID from the Vegas plugin config. Same for another events!
  victory:                          # [Optional] Defines victory event.
    fireworks: true                 # [Optional] Will there be fireworks for winning at the table's location?
    message-to:
      player: false
      all: false
      discord: false
    commands:
      execute-only-commands: false
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %prize%, %bet_price_rounded%, %prize_rounded%
      commands: []
    http:
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %prize%, %bet_price_rounded%, %prize_rounded%
      url: ''
      method: 'POST'
      headers:
        Content-Type: "application/json"
    music: 'victory'
  bonus:                            # [Optional] Defines bonus event.
    commands:
      execute-only-commands: false
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %prize%, %bet_price_rounded%, %prize_rounded%
      commands: []
    http:
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %prize%, %bet_price_rounded%, %prize_rounded%
      url: ''
      method: 'POST'
      headers:
        Content-Type: "application/json"
    music: 'bonus'
  defeat:                           # [Optional] Defines defeat event.
    message-to:
      player: false
      all: false
      discord: false
    commands:
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %bet_price_rounded%
      commands: []
    http:
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %bet_price_rounded%
      url: ''
      method: 'POST'
      headers:
        Content-Type: "application/json"
    music: 'defeat'
  bet:                               # [Optional]
    min-price: 100.0                 # [Optional]
    min-items-amount: 1              # [Optional]
    max-price: 999999999.0           # [Optional]
    max-items-amount: 64             # [Optional]
    money-bet:                       # [Optional]
      slots: 45                      # [Optional]
      material: 'ANVIL'              # [Required]
      custom-model-data: 0           # [Optional] Custom model data for the material.
      default-bet: 1000.0            # [Optional]
      change-per-click: 1000.0       # [Optional]
      change-per-click-big: 10000.0  # [Optional]
    spot:                            # [Required]
      slots: 20                      # [Required]
      material: 'BARRIER'            # [Required]
      custom-model-data: 0           # [Optional] Custom model data for the material.
  lever:                             # [Optional]
    slots: 11                        # [Optional]
    inactive:                        # [Optional]
      material: 'LEVER'              # [Required]
      custom-model-data: 0           # [Optional] Custom model data for the material.
    active:                          # [Optional]
      material: 'LEVER'              # [Required]
      custom-model-data: 0           # [Optional] Custom model data for the material.
    rolling:                         # [Optional]
      material: 'LEVER'              # [Required]
      custom-model-data: 0           # [Optional] Custom model data for the material.
  background:                        # [Optional]
    music: 'background_2'            # [Optional]
    music-button:                    # [Optional]
      slots: 45                      # [Optional]
      material-inactive: '1a906358c730c1ec753f34ccae7c39070b41917a5e235624f47fced715c4b98d'  # [Required]
      custom-model-data-inactive: 0                                                          # [Optional] Custom model data for the material.
      material-active: '86b1b9de36d25778fbbe6f5b8164c351be35c3711585c4081cc8398b003aa7c2'    # [Required]
      custom-model-data-active: 0                                                            # [Optional] Custom model data for the material.
    empty-slots: {}                  # [Optional] Slot: ['Material', Custom model data]  
```

#### Conclusion

Next comes the programming itself. Open the files, there will be a detailed description of what is responsible for what and how everything is arranged as compactly as possible! Good luck!




