# Train sounds: where they come from

Candidates for r39. The sound lab plays each one, and **Use** makes it the one the game plays.
Once a pair is chosen, the rest can go. `tools/make_train_sounds.py` rebuilds all of them from
the recordings below, and says how each was cut, looped and levelled.

## Recordings (Wikimedia Commons)

| Used in | Recording | Author | Licence |
|---|---|---|---|
| `departure_garratt`, `departure_whistle_garratt` | [South Australian Railways Garratt 406 starts off](https://commons.wikimedia.org/wiki/File:South_Australian_Railways_Garratt_406_starts_off.ogg) | SCHolar44 | CC0 1.0 |
| `departure_p8`, `departure_whistle_p8` | [WWS Prussian passenger steam locomotive P-8](https://commons.wikimedia.org/wiki/File:WWS_PrussianpassengersteamlocomotiveP-8.ogg) | Work With Sounds / Konrad Gutkowski | **CC BY 4.0: must be credited** |
| `departure_whistle_garratt`, `departure_whistle_p8` | [Parovoz sound](https://commons.wikimedia.org/wiki/File:Parovoz_sound.ogg) | Alex Lep | Public domain |
| `rails_ride`, `rails_ride_clacks` | [Complete train ride 4 minutes](https://commons.wikimedia.org/wiki/File:Complete_train_ride_4_minutes.ogg) | stephan (pdsounds.org) | Public domain |
| `rails_northern` | [Northern Trains 323239 DMSO A, Crewe to Manchester, Jan 2022](https://commons.wikimedia.org/wiki/File:Northern_Trains_323239_DMSO_A,_on_the_Crewe_to_Manchester_line,_Jan_2022.ogg) | TheFrog001 | CC0 1.0 |

If a P-8 departure is chosen, the credits must name *Work With Sounds / Konrad Gutkowski
(CC BY 4.0)*. No other candidate needs a credit.

## Synthesised

`rails_synth` is made entirely by the script: shaped noise for the wheel roar, and struck
resonances for the wheels crossing rail joints. `rails_ride_clacks` lays the same joint
clicks over the public-domain ride recording.
