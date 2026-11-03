# BGP's Vanilla+ Addon - tek plugin (data pack plugin'in icine tasindi)

Artik data pack yok. Sunucuya sadece `BGPs_Vanilla_Plus_Addon.jar` (plugin) ve resource pack gerekir.

## Kurulum
1. Bu klasoru GitHub'a at, `Actions` sekmesinde build'in bitmesini bekle, jar'i indir.
2. `plugins/` icine koy.
3. `world/datapacks/` icindeki eski data pack'i (BGPs_Vanilla_Plus_Addon_DataPack_v3) SIL ve eski plugin jar'ini kaldir.
4. Sunucuyu yeniden baslat.

## Esya verme (eski `function mace_drill:give/*` yerine)
`/bgpsgive <mace|book|template|elytra_leather|elytra_chainmail|elytra_iron|elytra_copper|elytra_golden|elytra_diamond|elytra_netherite|all> [oyuncu]`
Yetki: `bgps.give` (varsayilan op).

## Klasorler
- `prebuilt/`: calisan plugin'in derlenmis siniflari (tek fark: BGPsVanillaPlusAddon sinifinin `final` bayragi kaldirildi)
- `src/main/java/.../port/`: eski data pack'in Java karsiligi
- `src/main/resources/port/recipes/`: tarifler (data pack'ten birebir kopya)
- `legacy/`: eski data pack (yedek; jar'a girmez, test karsilastirmasinda kullanilir)
