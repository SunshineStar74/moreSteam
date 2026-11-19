Steam sounds, as mono .ogg (Ogg Vorbis) files. Which file plays for which event is set in
assets/moresteam/sounds.json; an event with several files picks one at random each time.

  bogey.idle_burst      - a cylinder of a standing train letting off a burst:
                          3_sec_burst, 5_sec_burst2, 5_sec_burst3, 5_sec_burst4
  bogey.stop_burst      - the cylinders blowing off steam as the train stops (drops to half
                          volume once the stop steam ends): 10_sec_burst
  bogey.departure       - the cylinders releasing pressure as the train pulls away: pressure_release
  bogey.pull_away_burst - a cylinder puffing while the train speeds up: 1_sec_no_loop_burst

Not used yet: 5_sec_burst, 7.71_countinuous_steam, continuous_burst.

Mono matters: Minecraft only plays sounds from a position in the world if the file is mono.
Keep file names lowercase; Minecraft rejects resource names with capital letters.
