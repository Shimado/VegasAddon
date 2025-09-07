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

In this config tree you can implement your data fields. You can see an example by opening config.yml of this project, where all the common fields listed below are processed by the Vegas plugin itself, and the fields that go specifically to this mode and are processed by this addon.

**[OPTIONAL]** fields - can be removed

**[REQUIRED]** fields

#### config.yml

```yaml
Example-settings:                    # [Required] Any key you want without spaces.
  mode-name: 'Drums'                 # [Required] The name that will be used. Should not be long, no spaces, only letters and numbers.
  permission: 'casino.drums'         # [Optional] The permission are needed to use the mode.
  gui-size: 6                        # [Optional] GUI size from 1 to 6. By default - 6.
  icon:                              # [Optional] It is necessary to output to the GUI. If there is no icon, then you can open it with the command.
    material: 'YELLOW_SHULKER_BOX'   # [Required] Material or head URL of the icon.
    custom-model-data: 0             # [Optional] Custom model data for the material.
    slot:                            # [Optional] Defines slots.
      all-games: 12                  # [Optional] Slot in the general game menu.  Can be 1 number or a list of numbers: 45 or [45, 53].
      one-game: 22                   # [Optional] Slot in the menu for one game.  Can be 1 number or a list of numbers: 45 or [45, 53].
  jackpot:                           # [Optional] Defines jackpot event.
    chance: 0.05                     # [Optional] Chance to hit the jackpot if you win.
    boost: 1.0                       # [Optional] Jackpot total bet booster. Jackpot Amount * Booster.
    charged-percentage: 10.0         # [Optional] The percentage of the bet will go towards the jackpot amount.
    message-to:                      # [Optional] Defines messages to this event. Same for another events!
      player: true                   # [Optional] - A message for you.
      all: true                      # [Optional] - A message for all players except the player.
      discord: true                  # [Optional] - A message to discord, if you have DiscordSRV plugin.
    commands:                        # [Optional] Defines commands to this event. Same for another events!
      execute-only-commands: false   # [Optional] Will only the commands be executed, without processing the issuance of the prize itself?
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %prize%, %bet_price_rounded%, %prize_rounded%
      commands: []                   # [Optional] Commands list to dispatch. For example: eco give %player_name% 100
    http:                            # [Optional] Defines http/https requests to this event. Same for another events!
      # ➥ Placeholders: %player_name%, %player_uuid%, %mode%, %bet_price%, %prize%, %bet_price_rounded%, %prize_rounded%
      url: ''                        # [Optional] A URL link to perform a request on. For example: https://www.youtube.com
      method: 'POST'                 # [Optional] Methods: GET, POST, PUT, DELETE
      headers:                       # [Optional] Example below
        Content-Type: "application/json"
    music: 'jackpot'                 # [Optional] Defines music to this event. You need to enter here music ID from the Vegas plugin config. Same for another events! Works only with BoomBox plugin.
  victory:                           # [Optional] Defines victory event.
    fireworks: true                  # [Optional] Will there be fireworks for winning at the table's location?
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
  bonus:                             # [Optional] Defines bonus event.
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
  defeat:                            # [Optional] Defines defeat event.
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
  bet:                               # [Required] Defines the parameters of the bet and the places for the bet.
    min-price: 100.0                 # [Optional] Minimum chip amount or cash bet.
    min-items-amount: 1              # [Optional] The minimum number of chips, or items, that a player can bet.
    max-price: 999999999.0           # [Optional] Maximum chip amount or cash bet.
    max-items-amount: 64             # [Optional] The maximum number of chips, or items, that a player can bet.
    money-bet:                       # [Optional] Defines the parameters of the money bet. If your chip type is CHIPS or ITEMS - do not specify this!
      slots: 45                      # [Optional] Slots for buttons. Can be 1 number or a list of numbers: 45 or [45, 53].
      material: 'ANVIL'              # [Required] Material or head URL of the icon.
      custom-model-data: 0           # [Optional] Custom model data for the material.
      default-bet: 1000.0            # [Optional] This is the standard bet amount. It can be changed by lower values. You can place a bet of 1000, then increase this value to, for example, 5000 and add 5000 to the total bet amount and the bet itself will be 6000.
      change-per-click: 1000.0       # [Optional] The amount that will be added or subtracted per click.
      change-per-click-big: 10000.0  # [Optional] The amount that will be added or subtracted per click with SHIFT.
    spot:                            # [Required] Place for betting chips/items/money.
      slots: 20                      # [Required] Slots of the button in the GUI itself. Can be 1 number or a list of numbers: 45 or [45, 53].
      material: 'BARRIER'            # [Required] Material or head URL of the icon.
      custom-model-data: 0           # [Optional] Custom model data for the material.
  lever:                             # [Optional] Defines the control levers. You can implement this yourself, in some Vegas modes they are not in the full list.
    slots: 11                        # [Optional] Slots of the button in the GUI itself. Can be 1 number or a list of numbers: 45 or [45, 53].
    inactive:                        # [Optional] Control lever condition.
      material: 'LEVER'              # [Required] Material or head URL of the icon.
      custom-model-data: 0           # [Optional] Custom model data for the material.
    active:                          # [Optional] Control lever condition.
      material: 'LEVER'              # [Required] Material or head URL of the icon.
      custom-model-data: 0           # [Optional] Custom model data for the material.
    rolling:                         # [Optional] Control lever condition.
      material: 'LEVER'              # [Required] Material or head URL of the icon.
      custom-model-data: 0           # [Optional] Custom model data for the material.
  background:                        # [Optional] Defines background items, buttons and empty slots.
    music: 'background_2'            # [Optional] Determines the background music. You need to enter here music ID from the Vegas plugin config. Works only with BoomBox plugin. 
    music-button:                    # [Optional] Allows you to turn on/off background music directly in game mode. Works only with BoomBox plugin.
      slots: 45                      # [Optional] Slots of the button in the GUI itself. Can be 1 number or a list of numbers: 45 or [45, 53].
      material-inactive: '1a906358c730c1ec753f34ccae7c39070b41917a5e235624f47fced715c4b98d'  # [Required] Material or head URL of the icon, when music is turned off.
      custom-model-data-inactive: 0                                                          # [Optional] Custom model data for the material.
      material-active: '86b1b9de36d25778fbbe6f5b8164c351be35c3711585c4081cc8398b003aa7c2'    # [Required] Material or head URL of the icon, when music is turned on.
      custom-model-data-active: 0                                                            # [Optional] Custom model data for the material.
    empty-slots: {}                  # [Optional] Slot: ['Material or head URL of the icon', Custom model data]  
```

#### en.yml (messages config)

```yaml
Example-settings:                                    # [Required] Any key you want without spaces.
  title: '         &4➌➌➌  &4&lDRUMS&r &4➌➌➌'       # [Required] Game mode GUI title. Must be unique: not duplicated with other GUI titles.
  icon:                                              # [Optional] It is necessary to output to the GUI. If there is no icon, then you can open it with the command.
    title: '&l➌➌➌  DRUMS  ➌➌➌'                     # [Required] The name of the game mode icon in the main menu.
    lore:                                            # [Optional] The description of the game mode icon in the main menu.
      - '&a• &aSingleplayer.'
      - '&e• &fYou can knock out different combinations'
      - '&e• &ffrom the game mode and depending on the combination'
      - '&e• &fand the material you get your prize!'
      - ' '
      - '&fVictory combinations:'
      - '&7➥ '
      - '&7➥ '
      - '&7➥ &5&l➎x➎ &r&eAll of the above - &fprize &6✕100'
      - '&7➥ &5&l➎x➊ &r&eOne line horizontally not in the center - &fprize &6✕6'
      - '&7➥ &5&l➎x➊ &r&eOne line horizontally in the center - &fprize &6✕25'
      - '&7➥ &5&l➊x➎ &r&eOne line vertically - &fprize &6✕10'
      - '&7➥ &eLine obliquely - &fprize &6✕10'
      - '&7➥ &eTwo oblique lines - &fprize &6✕50'
      - ' '
      - '&fBonus combinations:'
      - '&7➥ &5&l➋x➋ &r&eOne block - &fprize &6✕3.5'
      - '&7➥ '
      - ' '
      - '&e• &fIf any of these combinations are dragon egg,'
      - '&e• &fthen the main factor is multiplied &cby 5!'
  bet:                                               # [Required] Defines the parameters of the bet and the places for the bet.
    money-bet:                                       # [Optional] Defines the parameters of the money bet. If your chip type is CHIPS or ITEMS - do not specify this!
      title: '&6&lBET AMOUNT'                        # [Required] The name of the button in the game menu.
      lore:                                          # [Optional] The description of the button in the game menu.
        - '&fCurrent: &e%money%&6$'
        - '&fLeft click: &a+%money_click%$'
        - '&fLeft click + Shift: &a+%money_click_big%$'
        - '&fRight click: &c-%money_click%$'
        - '&fRight click + Shift: &c-%money_click_big%$'
    spot:                                            # [Required] Place for betting chips/items/money.
      title: '&6&lPlace your bet'                    # [Required] The name of the bet spot in the game menu.
      lore:                                          # [Optional] The description of the bet spot in the game menu.
        - '&fYou can place your chips!'
  lever:                                             # [Optional] Defines the control levers. You can implement this yourself, in some Vegas modes they are not in the full list.
    inactive:                                        # [Optional] Control lever condition.
      title: '&6&lPlace your bet to spin!'           # [Required] The name of the button in the game menu.
      lore: []                                       # [Optional] The description of the button in the game menu.
    active:                                          # [Optional] Control lever condition.
      title: '&6&lCLICK TO SPIN'                     # [Required] The name of the button in the game menu.
      lore: []                                       # [Optional] The description of the button in the game menu.
    rolling:                                         # [Optional] Control lever condition.
      title: '&6&lSCROLL IN...'                      # [Required] The name of the button in the game menu.
      lore: []                                       # [Optional] The description of the button in the game menu.
  background:                                        # [Optional] Defines background items, buttons and empty slots.
    music-button:                                    # [Optional] Allows you to turn on/off background music directly in game mode. Works only with BoomBox plugin.
      title-inactive: '&cMUSIC INACTIVE'             # [Required] The name of the button in the game menu, if music is turned off.
      title-active: '&aMUSIC ACTIVE'                 # [Required] The name of the button in the game menu, if music is turned on.
      lore: []                                       # [Optional] The description of the button in the game menu.
```

#### Conclusion

Next comes the programming itself. Open the files, there will be a detailed description of what is responsible for what and how everything is arranged as compactly as possible! Good luck!




