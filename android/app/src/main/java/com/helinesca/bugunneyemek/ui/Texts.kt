package com.helinesca.bugunneyemek.ui

/** Long Turkish texts, as written in the README ("Onboarding"). Edit them here and in the README together. */
object Texts {
    val intro = listOf(
        "Bugün ne yemek yapsam?" to
            "Her gün aynı soru, değil mi? 😊\n\n" +
            "Bu uygulama, ne pişireceğinize karar vermenize yardım eder.\n" +
            "Size birkaç yemek önerir, siz birini seçersiniz.\n" +
            "Gerisi sizin elinizin lezzetine kalmış.",
        "Nasıl çalışır?" to
            "1. \"Öner\" düğmesine dokunun.\n" +
            "2. Size üç yemek önerelim.\n" +
            "3. Beğenmezseniz \"Başka öner\" deyin.\n" +
            "4. İsterseniz ne kadar vaktiniz olduğunu ya da bugün üşendiğinizi söyleyin; önerileri ona göre seçelim.\n" +
            "5. \"Elimdekilerle öner\" düğmesiyle evinizde olan malzemelerle yapılabilecek yemekleri görün.\n\n" +
            "Yemeklerdeki bütün malzemeler, yakınınızdaki herhangi bir markette kolayca bulunabilecek şeylerdir.",
        "Bu bir tarif kitabı değil" to
            "Amacımız size en iyi tarifi vermek değil, fikir vermek.\n\n" +
            "Her yemeğin kısa ve basit bir tarifi var; ihtiyacı olan baksın diye. " +
            "Siz yemeği zaten biliyorsanız kendi bildiğiniz gibi yapın. Annenizin tarifi her zaman daha iyidir. 😉",
    )

    const val healthTitle = "⚠️ Sağlık uyarısı"
    const val health =
        "Bu uygulama, yemeği yiyecek herkesin sağlıklı olduğunu varsayar. " +
            "Hastalıkları, ilaçları ve özel diyetleri dikkate ALMAZ.\n\n" +
            "Sofranızdaki kişilerden birinde diyabet, tansiyon, kalp veya böbrek hastalığı, çölyak, gıda alerjisi, " +
            "hamilelik ya da doktorun önerdiği bir diyet varsa:\n\n" +
            "• Uygulamanın önerilerine güvenerek yemek seçmeyin.\n" +
            "• Ne yiyebileceğinizi doktorunuza veya diyetisyeninize danışın."

    const val importantTitle = "Önemli bilgiler"
    val important = listOf(
        "Genel" to listOf(
            "Tarifler ve öneriler genel bilgi amaçlıdır; tıbbi, diyet ya da beslenme tavsiyesi değildir.",
            "Miktarlar ve pişirme süreleri yaklaşıktır. Ocağınıza, fırınınıza ve malzemeye göre değişebilir; yemeğin pişip pişmediğini her zaman kendiniz kontrol edin.",
            "Kalori değerleri yaklaşık tahminlerdir; diyet planlaması için kullanmayın.",
            "Uygulamada hata olabilir. Size doğru gelmeyen bir bilgiye göre hareket etmeyin.",
        ),
        "Sağlık ve alerji" to listOf(
            "Alerjinizi ve yiyemediğiniz malzemeleri doğru ve eksiksiz girmeniz önemlidir; öneriler bu bilgilere göre yapılır.",
            "Paketli ürünler etikette görmediğiniz süt, gluten, yumurta veya kuruyemiş içerebilir. Uygulama etiket içeriğini bilemez; etiketi mutlaka kontrol edin.",
            "Öneriler, yemeği yiyecek herkesin sağlıklı olduğu varsayılarak yapılır. Uygulama sağlık durumunuzu bilmez. Diyabet, tansiyon, kalp, böbrek hastalığı, çölyak, hamilelik ya da özel bir diyet gibi bir durumunuz varsa beslenmeniz için doktorunuza veya diyetisyeninize danışın.",
            "1 yaşından küçük bebeklere bal verilmez. Hamileler, küçük çocuklar ve yaşlılar az pişmiş yumurtadan kaçınmalıdır.",
        ),
        "Pişirme ve saklama" to listOf(
            "Et, tavuk ve yumurtayı her zaman iyice pişirin.",
            "Çiğ et ve tavuk için ayrı kesme tahtası kullanın, ellerinizi sık sık yıkayın.",
            "Saklama süreleri genel önerilerdir. Buzdolabınızın ısısına ve yemeğin nasıl saklandığına göre değişir. Pişen yemeği soğuyunca kapalı kapta buzdolabına koyun, ısıttığınız yemeği tekrar saklamayın. Şüphelendiğiniz yemeği yemeyin.",
        ),
        "Mutfakta güvenlik" to listOf(
            "Sıcak yağ, bıçak, ocak ve fırın kullanırken dikkatli olun; çocukları uzak tutun.",
        ),
    )
    const val importantAccept = "Uygulamayı kullanarak bu bilgileri okuduğunuzu ve kabul ettiğinizi onaylarsınız."

    const val allergyWarning =
        "⚠️ Önemli\n\nSeçtiğiniz malzemeleri içeren yemekleri hiç önermeyiz. " +
            "Ancak paketli ürünler (bulyon, margarin, bisküvi, hazır sos vb.) etikette yazmayan süt, gluten, yumurta " +
            "veya kuruyemiş içerebilir.\n\nGerçek bir alerjiniz varsa, kullandığınız her paketli ürünün etiketini mutlaka kontrol edin."

    const val didItTitle = "Bir şey daha 😊"
    const val didIt =
        "Yemeği yaptıktan sonra \"Yaptım ✓\" düğmesine dokunun.\n\n" +
            "Böylece:\n" +
            "• Aynı yemeği iki hafta içinde tekrar önermeyiz.\n" +
            "• Arka arkaya hep kıymalı ya da hep tavuklu yemek çıkmaz.\n" +
            "• Uygulama zamanla sizin mutfağınızı öğrenir.\n\n" +
            "Dokunmazsanız da çalışır, ama öneriler sizi tanımaz."

    const val calorieSource = "Kalori verileri: USDA FoodData Central (ABD Tarım Bakanlığı, kamu malı veri)."
}
