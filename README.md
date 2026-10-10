# Bugün Ne Yemek Yapsam?

A Turkish-only app that answers the daily question "what should I cook today?".

**Status:** planning. There is no app code yet. The data is drafted: 124 pantry items, 153 dishes and a recipe for every dish (awaiting the owner's review).

**Platform:** Android only, modern versions only (no legacy Android support). The app's name, package ID and icon are not decided yet.

---

## The idea

The app is a **decider, not a cookbook**. Recipe sites answer "how do I make karnıyarık?". This app answers "what should I make today?". Most experienced cooks will only read the dish name, think "tamam, ondan yapayım" and close the app. The recipe is a fallback for people who need it.

What makes the suggestions good:
- **Memory:** a dish cooked recently is not suggested again for 10–14 days.
- **The situation:** energy, budget, time, guests, or "what I have at home" through its own button.
- **Personal taste:** allergies, dislikes and favourite ingredients, asked once at install.

## House rules for content

1. **Market pantry only.** Every dish uses only ingredients from `data/pantry.tsv`: things that are permanently stocked at A101, BİM or Şok. Weekly *Aktüel* and seasonal items don't count. These three chains are only the internal yardstick: user-facing text never names a store, and says "yakınınızdaki herhangi bir market" instead. World dishes are welcome as long as they follow this rule. Water, and household items such as kâğıt havlu, are assumed and not listed.
2. **Butter is not used.** Recipes use margarine instead: *kalıp margarin* for pastry and baking, *kase margarin* for everyday cooking.
3. **Keep it simple.** Use the fewest ingredients that still give the classic taste: one fat, one salça, no "optional" extras.
4. **Own words only.** Dish names, ingredient lists and methods are free to use. Recipe text is always written fresh, never copied, and no photos are taken from other sites.

## No photos (v1)

The app ships without dish photos. The suggestion card carries the weight instead:

```text
┌─────────────────────────────────┐
│ 🍲  Ezogelin çorbası            │
│ Naneli, bulgurlu, doyurucu      │
│ 45 dk · kolay · ucuz · ~280 kcal│
│ [ Tarif ]        [ Yaptım ✓ ]   │
└─────────────────────────────────┘
```

- One emoji per kind of dish, derived from `tur` and the main ingredients: 🍲 çorba, 🍖 kırmızı et, 🍗 tavuk, 🍳 yumurta, 🫘 bakliyat, 🥘 sebze, 🍚 pilav, 🍝 makarna, 🥗 salata-meze, 🥧 hamur işi, 🍮 tatlı, 🥔 garnitür.
- The one-line `aciklama` explains unfamiliar world dishes (e.g. Shakshuka: "Kuzey Afrika usulü baharatlı menemen").
- Why: a veteran pictures the dish from its name; nothing can look wrong; no "mine didn't look like the photo"; a smaller app; much less work.
- Photos may come in a later version if users ask. A test with Comfy Studio (RealVisXL V5) got generic soups nearly right but Turkish dishes wrong (ezogelin without grains, yayla with beans). The test prompts are kept in `photos/test_prompts.txt`.

## Recipe format

Each dish has two tabs.

**Tab 1: Malzemeler.** Every amount is shown two ways: first a kitchen measure, then metric.

```text
Pirinç ........ 1 su bardağı   · 180 g
Su ............ 1,5 su bardağı · 300 ml
Margarin ...... 1/8 paket      · 30 g
Domates ....... 2 adet (orta)  · ~250 g
```

| Measure | Value |
|---|---|
| Çay kaşığı | 5 ml |
| Tatlı kaşığı | 10 ml |
| Yemek kaşığı | 15 ml |
| Çay bardağı | 100 ml |
| Su bardağı (klasik) | 200 ml |
| Margarin: 1 paket / yarım / çeyrek / 1/8 | 250 g / 125 g / 62 g / 31 g |

- Grams per volume depend on the ingredient (a su bardağı of flour is not 200 g), so each dry ingredient gets its own conversion figures in `pantry.tsv`. Rounded, approximate figures are fine: a few grams either way make no difference to home cooking.
- Countable items stay as "adet" (or diş, demet, sap…), with an approximate weight, and are never halved. Recipes are never scaled down, so halves don't come up (see "Portions" below).
- "Göz kararı" is not used. Tuz and karabiber may say "damak zevkine göre".

**Tab 2: Yapılışı.** Numbered steps, one action per step, 1–2 lines each, ideally 5–8 steps. Steps don't repeat amounts.

The approved sample (bulgur pilavı) is in `data/samples/bulgur_pilavi.txt`.

### Writing recipes

Recipes are written by hand in `data/recipes/` (one text file per category) and checked by a build script:

```text
# 1 Mercimek çorbası
süre: 40
malzemeler:
- Kırmızı mercimek | 1 su bardağı
- Kuru soğan | 1 adet
- Su | 7 su bardağı
yapılışı:
1. Soğanı küçük doğra, mercimeği yıka.
2. ...
```

- The number and name after `#` must match `dishes.tsv`. The recipe is written for the dish's `taban_kisi`.
- Ingredient names must be exactly as in `pantry.tsv`; `Su` is the only extra.
- Deep-frying oil gets a third field: `- Ayçiçek yağı | 1/2 su bardağı | kızartma`. The app shows it as "(kızartma için)", and only 30% of it counts toward kcal, since most stays in the pan.
- Amounts: `1`, `1,5` or `1/2`, followed by a kitchen measure (çay kaşığı, tatlı kaşığı, yemek kaşığı, çay bardağı, su bardağı), the item's pantry unit (adet, diş, demet…), `g` or `ml`.
- Then run `python3 tools/build_recipes.py`. It refuses to write anything if a recipe breaks a rule, e.g. an ingredient outside the pantry, or half an egg. Otherwise it writes `recipe_ingredients.tsv` and `recipe_steps.tsv`, and updates `ana_malzemeler`, `sure_dk` and `kcal_porsiyon` in `dishes.tsv`.
- Never edit those generated columns and files by hand; edit the recipe and rerun the script.

## Calories

Each recipe shows an estimate under its title: `4 kişilik · 30 dk · ~400 kcal/porsiyon (tahmini)`.

- The app computes it; no one types a figure per dish. It turns every ingredient line into grams with the conversion columns, multiplies by `kcal_100g`, adds everything up and divides by the base size (`taban_kisi`). When a recipe changes, the figure updates by itself.
- Figures come from **USDA FoodData Central, SR Legacy (April 2018)**, which is US government public-domain data. `usda_id` records the exact entry, so every figure can be checked.
- `usda_eslesme` is `tam` when the USDA entry is the same food, and `yakın` when it is the closest equivalent (e.g. kaşar → gouda, lavaş → flour tortilla, sucuk → smoked beef sausage). Sumak and çörek otu have no entry (`yok`) and count as 0, which is negligible.
- Bone-in items (bütün tavuk, but/baget, kanat) are bought with bones, so `yenen_oran` in `pantry.tsv` (0.65–0.7) reduces them to the edible part before kcal is counted.
- It is always labelled "tahmini": frying oil absorption, fattier meat and portion size all change the real figure.
- Ayarlar › Hakkında credits the source (see "Ayarlar › Hakkında" below).

## Suggestion logic

1. **Filter:** drop any dish containing a hard-banned ingredient.
2. **Score** the rest:
   - up for loved ingredients and for matching the chosen situation
   - down for disliked ingredients, for being cooked recently, and for being similar to a recent dish (e.g. two kıymalı dishes in a row)
3. **Show the top 3** with some variety (not three kıyma dishes). "Başka öner" shows the next three.

**Number of people.** The household size (adults and children) is asked at install and used by default. When "Misafir var" is tapped, the app asks once how many people will be at the table:

```text
Normalde 4 kişisiniz. Bugün sofrada kaç kişi olacak?
Yetişkin: [ − 6 + ]   Çocuk: [ − 3 + ]
```

Guest mode also favours dishes tagged `misafir: evet`.

**Portions (never scaled down):**
- Each dish has a base size (`taban_kisi`): the smallest amount it is sensibly made in. 1 for dishes like sucuklu yumurta or tost, 2 for makarna, sote and similar, 4 for most home dishes, soups and spoon desserts, 8 for cakes, cookies and trays cut into slices (kek, brownie, revani, poğaça…). The recipe is written for that base.
- A child counts as half a portion (adjustable default).
- The app only multiplies: portions are rounded **up** to a whole multiple of the base. 1 person with base 4 → 4 portions; 6 people with base 4 → 8 portions (×2). Every amount is a whole multiple of the written recipe, so no halves appear.
- When more is cooked than needed, the recipe shows one line, based on `saklama`:

```text
Bu yemek en az 4 kişilik yapılır.
Kalanı buzdolabında 2-3 gün saklayıp ısıtarak yiyebilirsiniz.
```
  For `dondurulur` dishes it adds "Dondurucuda da saklanabilir."; for `taze` dishes it says instead that the dish is best eaten fresh.
- For single-person households, dishes with a small base or that keep well are favoured.
- Cooking times do not change. When a recipe is multiplied by 2 or more, it shows: "Daha büyük tencere / iki tepsi kullanın."

### The suggestion screen

Everything is pre-filled, so the user can simply press **"Öner"**.

**Always visible (one tap to change):**
1. **Kaç kişi:** e.g. "4 kişi (2 yetişkin, 2 çocuk)"
2. **Mutfak:** Türk / Dünya / Karışık
3. **Ne lazım:** Kahvaltılık / Çorba / Ana yemek / Pilav-makarna / Salata / Meze / Hamur işi / Atıştırmalık / Tatlı / Tam sofra (see "Ne lazım" sections)

**Folded under "Bugün farklı bir durum var mı?"** (closed by default):
4. **Süre:** 30 dk / 1 saat / Farketmez
5. **Enerji:** Üşeniyorum / Normal / Keyifliyim
6. **Bütçe:** Ay sonu / Normal
7. **Misafir var:** also opens the head count

**Defaults and memory:**
- Every option is first asked at install.
- After that, each option remembers the last choice.
- Suggested exception: "Misafir var" resets after "Yaptım ✓" or on the next day, because guests are usually there for one day only.

### "Elimdekilerle öner" (separate button)

A second button on the main screen, for the question "what can I make with what I have?".

- The user taps the main ingredients they have, grouped as in the pantry (et-tavuk, sebze, bakliyat, süt ürünleri…).
- Basics are assumed to be at home and are not asked: tuz, karabiber, yağ, salça, un, the common spices, and su.
- Suggestions are dishes whose main ingredients are all, or nearly all, covered. A dish missing one or two items shows them, e.g. "Eksik: sivri biber".
- The same filters apply as on the normal screen (bans, Kaç kişi, Mutfak, Ne lazım, memory).
- The selection is remembered, with a "Temizle" button to start over.

## Onboarding (at install)

The first five screens explain what the app is for, what it is not, and its limits, before any questions are asked. The taste questions after them can be skipped and changed later in Ayarlar.

**Screen 1: The aim**
```text
Bugün ne yemek yapsam?

Her gün aynı soru, değil mi? 😊

Bu uygulama, ne pişireceğinize karar vermenize yardım eder.
Size birkaç yemek önerir, siz birini seçersiniz.
Gerisi sizin elinizin lezzetine kalmış.
```

**Screen 2: How it works**
```text
Nasıl çalışır?

1. "Bugün ne yemek yapsam?" düğmesine dokunun.
2. Size üç yemek önerelim.
3. Beğenmezseniz "Başka öner" deyin.
4. İsterseniz ne kadar vaktiniz olduğunu ya da bugün
   üşendiğinizi söyleyin; önerileri ona göre seçelim.
5. "Elimdekilerle öner" düğmesiyle evinizde olan
   malzemelerle yapılabilecek yemekleri görün.

Yemeklerdeki bütün malzemeler, yakınınızdaki herhangi
bir markette kolayca bulunabilecek şeylerdir.
```

**Screen 3: What it is not**
```text
Bu bir tarif kitabı değil

Amacımız size en iyi tarifi vermek değil, fikir vermek.

Her yemeğin kısa ve basit bir tarifi var; ihtiyacı olan
baksın diye. Siz yemeği zaten biliyorsanız kendi bildiğiniz
gibi yapın. Annenizin tarifi her zaman daha iyidir. 😉
```

**Screen 4: Health warning** (its own screen, shown in a strong red/amber style)
```text
⚠️ Sağlık uyarısı

Bu uygulama, yemeği yiyecek herkesin sağlıklı olduğunu
varsayar. Hastalıkları, ilaçları ve özel diyetleri
dikkate ALMAZ.

Sofranızdaki kişilerden birinde diyabet, tansiyon, kalp
veya böbrek hastalığı, çölyak, gıda alerjisi, hamilelik
ya da doktorun önerdiği bir diyet varsa:

• Uygulamanın önerilerine güvenerek yemek seçmeyin.
• Ne yiyebileceğinizi doktorunuza veya diyetisyeninize
  danışın.

[ Anladım ]
```

**Screen 5: Important information** (scrollable; the "Okudum, anladım" button becomes active once the user has scrolled to the end; no checkbox wall)
```text
Önemli bilgiler

Genel
• Tarifler ve öneriler genel bilgi amaçlıdır; tıbbi, diyet
  ya da beslenme tavsiyesi değildir.
• Miktarlar ve pişirme süreleri yaklaşıktır. Ocağınıza,
  fırınınıza ve malzemeye göre değişebilir; yemeğin
  pişip pişmediğini her zaman kendiniz kontrol edin.
• Kalori değerleri yaklaşık tahminlerdir; diyet planlaması
  için kullanmayın.
• Uygulamada hata olabilir. Size doğru gelmeyen bir
  bilgiye göre hareket etmeyin.

Sağlık ve alerji
• Alerjinizi ve yiyemediğiniz malzemeleri doğru ve eksiksiz
  girmeniz önemlidir; öneriler bu bilgilere göre yapılır.
• Paketli ürünler etikette görmediğiniz süt, gluten, yumurta
  veya kuruyemiş içerebilir. Uygulama etiket içeriğini
  bilemez; etiketi mutlaka kontrol edin.
• Öneriler, yemeği yiyecek herkesin sağlıklı olduğu
  varsayılarak yapılır. Uygulama sağlık durumunuzu bilmez.
  Diyabet, tansiyon, kalp, böbrek hastalığı, çölyak,
  hamilelik ya da özel bir diyet gibi bir durumunuz varsa
  beslenmeniz için doktorunuza veya diyetisyeninize danışın.
• 1 yaşından küçük bebeklere bal verilmez. Hamileler, küçük
  çocuklar ve yaşlılar az pişmiş yumurtadan kaçınmalıdır.

Pişirme ve saklama
• Et, tavuk ve yumurtayı her zaman iyice pişirin.
• Çiğ et ve tavuk için ayrı kesme tahtası kullanın,
  ellerinizi sık sık yıkayın.
• Saklama süreleri genel önerilerdir. Buzdolabınızın ısısına
  ve yemeğin nasıl saklandığına göre değişir. Pişen yemeği
  soğuyunca kapalı kapta buzdolabına koyun, ısıttığınız
  yemeği tekrar saklamayın. Şüphelendiğiniz yemeği yemeyin.

Mutfakta güvenlik
• Sıcak yağ, bıçak, ocak ve fırın kullanırken dikkatli olun;
  çocukları uzak tutun.

Uygulamayı kullanarak bu bilgileri okuduğunuzu ve
kabul ettiğinizi onaylarsınız.

[ Okudum, anladım ]        Kullanım Koşulları ›
```

- The **Kullanım Koşulları** page is short and hosted on GitHub Pages next to the privacy policy, which Google Play requires anyway.
- Storage lines on recipes are always phrased as suggestions ("saklayabilirsiniz"), never as promises.
- Content stays conservative: fridge 2–3 days, and meat, chicken and eggs are always cooked through.

**Then the taste questions:**

1. **Alerjim var / yiyemem → hard ban.** Includes group buttons (gluten, süt ürünleri, yumurta, fındık-ceviz, susam) that expand to the matching pantry items.
2. **Sevmem → soft ban.** These dishes are ranked low, but not removed.
3. **Bayılırım → boost.**
4. "Et yiyor musunuz?" (evet / az / hiç)
5. "Evde kaç kişisiniz?" (Yetişkin: [ − 2 + ]  Çocuk: [ − 2 + ])
   Then the defaults for the suggestion screen: Mutfak, Ne lazım, Süre, Enerji and Bütçe (see "The suggestion screen" above).
6. **The "Yaptım" explanation:**

```text
Bir şey daha 😊

Yemeği yaptıktan sonra "Yaptım ✓" düğmesine dokunun.

Böylece:
• Aynı yemeği iki hafta içinde tekrar önermeyiz.
• Arka arkaya hep kıymalı ya da hep tavuklu yemek çıkmaz.
• Uygulama zamanla sizin mutfağınızı öğrenir.

Dokunmazsanız da çalışır, ama öneriler sizi tanımaz.
```

**Allergy warning.** It appears on the allergy screen, and for users with an allergy it is also shown as a small ⚠️ line on any recipe that uses a packaged product:

```text
⚠️ Önemli

Seçtiğiniz malzemeleri içeren yemekleri hiç önermeyiz.
Ancak paketli ürünler (bulyon, margarin, bisküvi, hazır sos vb.)
etikette yazmayan süt, gluten, yumurta veya kuruyemiş içerebilir.

Gerçek bir alerjiniz varsa, kullandığınız her paketli ürünün
etiketini mutlaka kontrol edin.
```

**Optional idea:** on the next app open, a single card could ask "Dün **X** önermiştik, yaptınız mı?" with **Evet ✓ / Hayır**. There would be no notifications.

## Ayarlar › Hakkında

A permanent page, so the install warnings can be reread at any time. It contains:
- the app name and version
- the full texts of the health warning (Screen 4) and "Önemli bilgiler" (Screen 5)
- links to Kullanım Koşulları and the privacy policy
- the calorie source: "Kalori verileri: USDA FoodData Central (ABD Tarım Bakanlığı, kamu malı veri)."

## Health conditions: a deliberate limit

The app does **not** give suggestions for specific illnesses: no "diyabete uygun", "tansiyon dostu" or "böbrek hastalarına" labels or modes. That would turn general cooking ideas into health advice, which is risky: a wrong suggestion can harm someone, liability rises sharply, and Google Play has stricter rules for health apps.

What users can do instead is filter by ingredient themselves, using the existing bans (e.g. the gluten group, or banning şeker). The choice is theirs, and the app makes no health claim about it.

## "Ne lazım" sections

Each dish has one or more sections in `ne_lazim` (`dishes.tsv`). The screen offers them as choices:

| Section | What goes in |
|---|---|
| Kahvaltılık | All egg dishes, **every dish with sucuk or sosis** (owner's rule: strictly breakfast; the exceptions are pizza, which fits any time of day, and sucuklu carbonara, an evening pasta listed under ana yemek and pilav-makarna), krep, pankek, pişi, gözleme, poğaça, sigara böreği, milföy, mısır ekmeği |
| Çorba | The soups |
| Ana yemek | Et, tavuk, köfte, sebze, zeytinyağlı and bakliyat dishes; hamburger, dürüm; mantı; tepsi börekleri and pide |
| Pilav-makarna | Pilavs and pasta |
| Salata | Salads |
| Meze | Haydari, humus, patlıcan salatası, havuç tarator, cacık, şakşuka, mercimek köftesi, falafel |
| Hamur işi | Pizzas (also kahvaltılık, ana yemek and atıştırmalık), börekler, gözleme, pişi, poğaça, pide, milföy, mısır ekmeği, mantı |
| Atıştırmalık | Çay saati / gün food: poğaça, börekler, kısır, mercimek köftesi, patates and Rus salatası, humus, falafel, kek and kurabiyeler |
| Tatlı | The desserts |
| Garnitür | Not a choice on its own; patates kızartması and patates püresi, used as a yanına in Tam sofra |
| Tam sofra | A combination: one çorba + one ana yemek + one yanına (pilav-makarna, salata, meze or garnitür) |

- Egg dishes are never ana yemek: in Turkish home cooking they are breakfast food.
- The groupings follow how Turkish recipe sites (Nefis Yemek Tarifleri, Yemek.com) and common gün and çay saati menus group these dishes.

## Decided defaults

Agreed with the owner. The allergy groups are a required feature.

1. **Allergy groups:** `data/alerji_gruplari.tsv` maps gluten, süt ürünleri, yumurta, fındık-ceviz, susam and balık to pantry items. Its `not` column marks packaged items that usually or may contain the allergen (bulyon, margarin, bisküvi…).
2. **Basics assumed at home for "Elimdekilerle öner":** `data/temel_malzemeler.tsv` (salt, spices, oils, salça, un, şeker, sirke, karbonat, kabartma tozu, kuru soğan, sarımsak).
3. **Small rules:**
   - "Et" in "Et yiyor musunuz?" means **red meat only**: dana kıyma, dana kuşbaşı and dondurulmuş köfte. Chicken, ton balığı and şarküteri (sucuk, sosis, salam) are not "et".
   - "az" → red-meat dishes are suggested at most every other day. "hiç" → red-meat dishes are never suggested.
   - Seasons: May–September is `yaz`, November–March is `kış`; April and October count as both.
   - Memory: a cooked dish is not suggested again for 12 days.
4. **Technology:** Kotlin + Jetpack Compose, fully offline, no internet permission. The data files are bundled into the app at build time.
5. **Test build only:** a "Not al" button on each recipe saves the owner's notes on the phone during the test weeks; they are pulled with adb afterwards and used to fix the recipes. Not in the release build.

## Data files

| File | Contents |
|---|---|
| `data/pantry.tsv` | The ingredient whitelist, with rounded conversions. Columns: `id`, `grup`, `malzeme`, `not`, `g_su_bardagi`, `g_yemek_kasigi`, `g_cay_kasigi` (grams per measure), `birim` (adet, diş, demet, sap, dilim, paket, kutu…) `g_birim` (grams per unit), `kcal_100g`, `usda_id`, `usda_eslesme` and `yenen_oran` (see "Calories" below). Items bought by weight (kıyma, kuşbaşı, tavuk göğsü…) have no conversion and are given in grams. |
| `data/dishes.tsv` | The dish catalog (draft, under review). Columns below. |
| `data/alerji_gruplari.tsv` | Proposed allergy groups → pantry items (see above). |
| `data/temel_malzemeler.tsv` | Proposed basics assumed at home (see above). |
| `data/samples/` | Approved sample recipes in the final two-tab format. |
| `data/recipes/*.txt` | Hand-written recipes, the source for everything below. |
| `data/recipe_ingredients.tsv` | Generated. One row per ingredient line: `dish_id`, `sira`, `pantry_id`, `malzeme`, `miktar`, `olcu`, `gram`, `kizartma`. |
| `data/recipe_steps.tsv` | Generated. One row per step: `dish_id`, `sira`, `metin`. |
| `tools/build_recipes.py` | Checks the recipes and builds the generated files. |

`dishes.tsv` columns:

| Column | Meaning |
|---|---|
| `id` | Dish number |
| `ad` | Dish name |
| `tur` | çorba, ana yemek, pilav, makarna, salata-meze, hamur işi, tatlı or garnitür |
| `ne_lazim` | The "Ne lazım" sections the dish appears in, comma-separated (see above). |
| `ana_malzemeler` | Pantry IDs, comma-separated. Lists every ingredient except tuz, karabiber, ayçiçek yağı and su, because bans and loves depend on it. It is updated whenever the dish's recipe is written. |
| `sure` | kısa (≤30 dk), orta (30–60 dk), uzun (>60 dk, or needs overnight soaking) |
| `zahmet` | kolay, normal or uğraştırır |
| `maliyet` | ucuz or normal |
| `misafir` | `evet` if the dish suits guests |
| `mevsim` | yaz, kış or empty (all year) |
| `mutfak` | Türk or Dünya |
| `taban_kisi` | Base size: 1, 2, 4 or 8 portions. The recipe is written for this, and only multiplied. |
| `saklama` | `dondurulur` (fridge 2–3 days, also freezes), `buzdolabı` (fridge 2–3 days) or `taze` (best eaten fresh) |
| `uyari` | Per-recipe safety notes, comma-separated: `bal` → "⚠️ 1 yaşından küçük bebeklere bal verilmez."; `az_pismis_yumurta` → "⚠️ Hamileler, küçük çocuklar ve yaşlılar için yumurtayı iyice pişirin." `bal` is kept up to date by the build script from the recipe. |
| `sure_dk` | Generated from the recipe: total time in minutes. |
| `kcal_porsiyon` | Generated from the recipe: estimated kcal per portion, rounded to 10. |
| `aciklama` | One short line, shown under the dish name |

## License

- **Code** (the app and `tools/`): MIT © helinesca. See `LICENSE`.
- **Content** (everything in `data/`: the dish catalog, recipes, pantry list and conversions): **CC BY-NC 4.0** © helinesca. It may be used and shared with credit, but **not for commercial purposes**. See `data/LICENSE` or <https://creativecommons.org/licenses/by-nc/4.0/>.
- The calorie figures in `pantry.tsv` come from USDA FoodData Central, which is public-domain data.
