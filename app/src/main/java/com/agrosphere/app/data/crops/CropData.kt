package com.agrosphere.app.data.crops

data class CropVariety(val cropName: String, val varieties: List<String> = emptyList())
data class CropCategory(val name: String, val crops: List<CropVariety>)

object CropData {

    val categories: List<CropCategory> = listOf(

        CropCategory("Cereals & Grains", listOf(
            CropVariety("Rice", listOf("Basmati", "Jasmine", "Long Grain", "Short Grain", "Arborio", "Sona Masuri", "Ponni", "Black Rice", "Brown Rice", "Wild Rice", "Glutinous (Sticky)", "Red Rice")),
            CropVariety("Wheat", listOf("Hard Red Winter", "Hard Red Spring", "Soft Red Winter", "Hard White", "Soft White", "Durum", "Spelt", "Emmer", "Einkorn", "Khorasan (Kamut)")),
            CropVariety("Maize (Corn)", listOf("Dent", "Flint", "Sweet Corn", "Popcorn", "Flour Corn", "Waxy", "Baby Corn", "Field Corn", "Blue Corn")),
            CropVariety("Barley", listOf("Two-row", "Six-row", "Hulless", "Malting", "Feed")),
            CropVariety("Oats", listOf("Common", "Hulless", "Red", "Black", "Naked")),
            CropVariety("Sorghum", listOf("Grain Sorghum (Milo)", "Forage Sorghum", "Sweet Sorghum", "Broomcorn")),
            CropVariety("Millet", listOf("Pearl (Bajra)", "Finger (Ragi)", "Foxtail", "Proso", "Barnyard", "Kodo", "Little Millet", "Browntop")),
            CropVariety("Rye", listOf("Winter Rye", "Spring Rye")),
            CropVariety("Triticale"),
            CropVariety("Teff"),
            CropVariety("Buckwheat"),
            CropVariety("Quinoa", listOf("White", "Red", "Black")),
            CropVariety("Amaranth"),
            CropVariety("Fonio"),
        )),

        CropCategory("Pulses & Legumes", listOf(
            CropVariety("Soybean", listOf("Yellow", "Black", "Edamame", "Non-GMO Food Grade")),
            CropVariety("Chickpea", listOf("Desi (Bengal Gram)", "Kabuli", "Green")),
            CropVariety("Lentil", listOf("Red (Masoor)", "Green", "Brown", "Black (Beluga)", "Puy", "Yellow")),
            CropVariety("Pea", listOf("Garden Pea", "Snow Pea", "Snap Pea", "Field/Dry Pea", "Split Pea")),
            CropVariety("Pigeon Pea (Tur/Arhar)"),
            CropVariety("Mung Bean (Green Gram)"),
            CropVariety("Urad (Black Gram)"),
            CropVariety("Cowpea (Black-eyed Pea)", listOf("Black-eyed", "Crowder", "Yardlong")),
            CropVariety("Common Bean", listOf("Kidney", "Pinto", "Navy", "Black", "Cranberry", "Great Northern", "Lima", "Anasazi")),
            CropVariety("Faba Bean (Broad Bean)"),
            CropVariety("Adzuki Bean"),
            CropVariety("Lupin", listOf("Sweet White", "Narrow-leafed", "Yellow")),
            CropVariety("Moth Bean"),
            CropVariety("Horsegram (Kulthi)"),
            CropVariety("Bambara Groundnut"),
        )),

        CropCategory("Oilseeds", listOf(
            CropVariety("Groundnut (Peanut)", listOf("Runner", "Virginia", "Spanish", "Valencia")),
            CropVariety("Rapeseed / Canola", listOf("Winter Canola", "Spring Canola", "HEAR (Industrial)")),
            CropVariety("Mustard", listOf("Yellow", "Brown", "Black", "Indian Mustard")),
            CropVariety("Sunflower", listOf("Oilseed (NuSun)", "Confection", "High-oleic")),
            CropVariety("Sesame", listOf("White", "Black", "Brown")),
            CropVariety("Safflower"),
            CropVariety("Niger Seed"),
            CropVariety("Linseed (Flax)", listOf("Brown", "Golden")),
            CropVariety("Castor"),
            CropVariety("Camelina"),
            CropVariety("Oil Palm", listOf("Tenera", "Dura", "Pisifera")),
            CropVariety("Coconut", listOf("Tall", "Dwarf", "Hybrid", "King")),
        )),

        CropCategory("Vegetables — Leafy", listOf(
            CropVariety("Spinach", listOf("Savoy", "Semi-Savoy", "Flat-leaf", "Baby")),
            CropVariety("Lettuce", listOf("Iceberg", "Romaine", "Butterhead", "Looseleaf", "Batavian", "Oakleaf", "Little Gem")),
            CropVariety("Kale", listOf("Curly", "Lacinato (Dinosaur)", "Red Russian", "Redbor")),
            CropVariety("Swiss Chard", listOf("Fordhook Giant", "Rainbow", "Ruby Red")),
            CropVariety("Mustard Greens"),
            CropVariety("Arugula (Rocket)"),
            CropVariety("Pak Choi (Bok Choy)"),
            CropVariety("Napa Cabbage"),
            CropVariety("Collard Greens"),
            CropVariety("Fenugreek Leaves (Methi)"),
            CropVariety("Moringa (Drumstick Leaves)"),
            CropVariety("Malabar Spinach"),
            CropVariety("Amaranth Leaves"),
            CropVariety("Watercress"),
            CropVariety("Sorrel"),
        )),

        CropCategory("Vegetables — Brassicas", listOf(
            CropVariety("Cabbage", listOf("Green", "Red", "Savoy", "Pointed", "January King")),
            CropVariety("Cauliflower", listOf("White", "Orange", "Purple", "Green (Romanesco)")),
            CropVariety("Broccoli", listOf("Calabrese", "Sprouting", "Purple Sprouting", "Broccolini")),
            CropVariety("Brussels Sprouts"),
            CropVariety("Kohlrabi", listOf("Green", "Purple")),
            CropVariety("Turnip", listOf("Purple Top", "White", "Hakurei", "Tokyo")),
            CropVariety("Rutabaga (Swede)"),
            CropVariety("Daikon Radish"),
            CropVariety("Horseradish"),
        )),

        CropCategory("Vegetables — Root & Tuber", listOf(
            CropVariety("Potato", listOf("Russet", "Yukon Gold", "Red", "Fingerling", "Purple/Blue", "White", "Kennebec", "Maris Piper", "Desiree")),
            CropVariety("Sweet Potato", listOf("Orange (Beauregard)", "Purple (Okinawan)", "White", "Japanese (Satsumaimo)")),
            CropVariety("Carrot", listOf("Nantes", "Imperator", "Chantenay", "Danvers", "Purple", "Rainbow", "Baby")),
            CropVariety("Radish", listOf("Red Globe", "French Breakfast", "Watermelon", "Black Spanish", "Daikon")),
            CropVariety("Beetroot", listOf("Detroit Dark Red", "Golden", "Chioggia", "Cylindra")),
            CropVariety("Cassava (Manioc/Yuca)", listOf("Sweet", "Bitter")),
            CropVariety("Yam", listOf("White", "Yellow", "Water Yam", "Purple")),
            CropVariety("Taro (Colocasia)"),
            CropVariety("Ginger"),
            CropVariety("Turmeric"),
            CropVariety("Galangal"),
            CropVariety("Jerusalem Artichoke"),
            CropVariety("Lotus Root"),
            CropVariety("Arrowroot"),
        )),

        CropCategory("Vegetables — Allium", listOf(
            CropVariety("Onion", listOf("Yellow", "Red", "White", "Sweet (Vidalia)", "Spanish", "Cipollini", "Pearl")),
            CropVariety("Garlic", listOf("Softneck", "Hardneck", "Elephant Garlic", "Black Garlic")),
            CropVariety("Shallot"),
            CropVariety("Leek"),
            CropVariety("Spring Onion / Scallion"),
            CropVariety("Chive"),
            CropVariety("Garlic Chives"),
            CropVariety("Welsh Onion"),
        )),

        CropCategory("Vegetables — Fruiting", listOf(
            CropVariety("Tomato", listOf("Beefsteak", "Roma", "Cherry", "Grape", "Heirloom", "San Marzano", "Brandywine", "Plum", "Green Zebra", "Yellow")),
            CropVariety("Chilli Pepper", listOf("Cayenne", "Jalapeño", "Habanero", "Ghost (Bhut Jolokia)", "Scotch Bonnet", "Bird's Eye", "Serrano", "Thai", "Kashmiri", "Guntur", "Poblano")),
            CropVariety("Bell Pepper (Capsicum)", listOf("Green", "Red", "Yellow", "Orange", "Purple", "Chocolate")),
            CropVariety("Brinjal (Eggplant)", listOf("Globe", "Italian", "Japanese", "Thai", "White", "Graffiti", "Indian")),
            CropVariety("Okra (Lady's Finger)", listOf("Clemson Spineless", "Red Burgundy")),
            CropVariety("Tomatillo"),
            CropVariety("Cape Gooseberry"),
        )),

        CropCategory("Vegetables — Gourds", listOf(
            CropVariety("Cucumber", listOf("Slicing", "Pickling", "English", "Persian", "Armenian", "Lemon")),
            CropVariety("Pumpkin", listOf("Jack-o-Lantern", "Sugar Pie", "Cinderella", "Cushaw", "Kabocha")),
            CropVariety("Winter Squash", listOf("Butternut", "Acorn", "Spaghetti", "Hubbard", "Delicata", "Buttercup")),
            CropVariety("Summer Squash / Zucchini", listOf("Green Zucchini", "Yellow", "Crookneck", "Pattypan")),
            CropVariety("Watermelon", listOf("Seeded", "Seedless", "Yellow Flesh", "Sugar Baby", "Crimson Sweet")),
            CropVariety("Muskmelon (Cantaloupe)", listOf("Athena", "Hales Best", "Charentais", "Galia")),
            CropVariety("Bottle Gourd (Lauki)"),
            CropVariety("Bitter Gourd (Karela)"),
            CropVariety("Ridge Gourd (Turai)"),
            CropVariety("Snake Gourd"),
            CropVariety("Sponge Gourd (Luffa)"),
            CropVariety("Ash Gourd (Winter Melon)"),
            CropVariety("Ivy Gourd (Tindora)"),
        )),

        CropCategory("Fruits — Tropical", listOf(
            CropVariety("Banana", listOf("Cavendish", "Plantain", "Red Banana", "Lady Finger", "Robusta", "Nendran", "Blue Java")),
            CropVariety("Mango", listOf("Alphonso", "Kesar", "Dasheri", "Langra", "Totapuri", "Banganapalli", "Sindhri", "Chaunsa", "Ataulfo", "Kent", "Keitt", "Tommy Atkins", "Haden")),
            CropVariety("Pineapple", listOf("Smooth Cayenne", "MD-2 (Gold)", "Queen", "Red Spanish")),
            CropVariety("Papaya", listOf("Red Lady", "Solo", "Maradol", "Hawaiian")),
            CropVariety("Guava", listOf("White", "Pink", "Allahabad Safeda", "Lalit", "Thai")),
            CropVariety("Jackfruit"),
            CropVariety("Avocado", listOf("Hass", "Fuerte", "Reed", "Pinkerton", "Bacon", "Lamb Hass")),
            CropVariety("Dragon Fruit (Pitaya)", listOf("White Flesh", "Red Flesh", "Yellow")),
            CropVariety("Passion Fruit", listOf("Purple", "Yellow")),
            CropVariety("Lychee"),
            CropVariety("Rambutan"),
            CropVariety("Longan"),
            CropVariety("Mangosteen"),
            CropVariety("Pomegranate", listOf("Wonderful", "Ganesh", "Bhagwa", "Kandhari")),
            CropVariety("Date", listOf("Medjool", "Deglet Noor", "Barhi", "Halawi", "Zahidi")),
            CropVariety("Fig", listOf("Black Mission", "Brown Turkey", "Kadota", "Calimyrna")),
            CropVariety("Persimmon", listOf("Fuyu", "Hachiya", "Sharon Fruit")),
            CropVariety("Custard Apple (Sitaphal)"),
            CropVariety("Sapodilla (Chikoo)"),
            CropVariety("Star Fruit (Carambola)"),
            CropVariety("Tamarind"),
            CropVariety("Wood Apple (Bael)"),
            CropVariety("Jujube (Ber)"),
        )),

        CropCategory("Fruits — Citrus", listOf(
            CropVariety("Orange", listOf("Valencia", "Navel", "Blood", "Cara Cara", "Hamlin")),
            CropVariety("Mandarin", listOf("Clementine", "Satsuma", "Tangerine", "Kinnow", "Nagpur", "Dancy")),
            CropVariety("Lemon", listOf("Eureka", "Lisbon", "Meyer", "Ponderosa")),
            CropVariety("Lime", listOf("Persian (Tahiti)", "Key", "Kaffir", "Finger Lime")),
            CropVariety("Grapefruit", listOf("Ruby Red", "Pink", "White (Marsh)", "Star Ruby")),
            CropVariety("Pomelo"),
            CropVariety("Sweet Lime (Mosambi)"),
            CropVariety("Citron"),
            CropVariety("Yuzu"),
            CropVariety("Calamansi"),
        )),

        CropCategory("Fruits — Pome & Stone", listOf(
            CropVariety("Apple", listOf("Gala", "Fuji", "Granny Smith", "Honeycrisp", "Red Delicious", "Golden Delicious", "Pink Lady", "Braeburn", "McIntosh", "Kashmiri")),
            CropVariety("Pear", listOf("Bartlett", "Anjou", "Bosc", "Comice", "Asian Pear (Nashi)", "Conference", "Williams")),
            CropVariety("Quince"),
            CropVariety("Peach", listOf("Freestone", "Clingstone", "Donut (Saturn)", "White")),
            CropVariety("Nectarine"),
            CropVariety("Plum", listOf("European", "Japanese", "Damson", "Mirabelle", "Greengage", "Santa Rosa")),
            CropVariety("Apricot", listOf("Blenheim", "Moorpark", "Tilton", "Tomcot")),
            CropVariety("Cherry", listOf("Bing", "Rainier", "Sweetheart", "Sour (Montmorency)", "Lapins", "Stella")),
        )),

        CropCategory("Fruits — Berries", listOf(
            CropVariety("Strawberry", listOf("June-bearing", "Everbearing", "Day-neutral", "Albion", "Camarosa", "Chandler")),
            CropVariety("Blueberry", listOf("Highbush", "Lowbush", "Rabbiteye", "Half-high")),
            CropVariety("Raspberry", listOf("Red", "Black", "Yellow", "Purple", "Summer-bearing", "Fall-bearing")),
            CropVariety("Blackberry", listOf("Erect", "Trailing", "Thornless")),
            CropVariety("Cranberry"),
            CropVariety("Gooseberry", listOf("European", "American")),
            CropVariety("Elderberry"),
            CropVariety("Mulberry", listOf("White", "Black", "Red")),
            CropVariety("Goji Berry"),
            CropVariety("Sea Buckthorn"),
        )),

        CropCategory("Nuts", listOf(
            CropVariety("Almond", listOf("Nonpareil", "Carmel", "Mission", "Padre", "Marcona")),
            CropVariety("Walnut", listOf("English (Persian)", "Black", "Chandler", "Hartley")),
            CropVariety("Cashew"),
            CropVariety("Hazelnut (Filbert)"),
            CropVariety("Pistachio", listOf("Kerman", "Sirora", "Aegina")),
            CropVariety("Pecan", listOf("Stuart", "Pawnee", "Desirable", "Cheyenne")),
            CropVariety("Macadamia"),
            CropVariety("Brazil Nut"),
            CropVariety("Chestnut", listOf("Sweet (European)", "Chinese", "Japanese")),
            CropVariety("Pine Nut (Pignoli)"),
        )),

        CropCategory("Herbs & Spices", listOf(
            CropVariety("Basil", listOf("Sweet (Genovese)", "Thai", "Holy (Tulsi)", "Lemon", "Purple")),
            CropVariety("Mint", listOf("Peppermint", "Spearmint", "Chocolate", "Pineapple", "Apple")),
            CropVariety("Coriander / Cilantro"),
            CropVariety("Parsley", listOf("Flat-leaf (Italian)", "Curly", "Hamburg")),
            CropVariety("Dill"),
            CropVariety("Oregano"),
            CropVariety("Thyme", listOf("Common", "Lemon", "Creeping")),
            CropVariety("Sage"),
            CropVariety("Rosemary"),
            CropVariety("Tarragon"),
            CropVariety("Bay Leaf"),
            CropVariety("Lemongrass"),
            CropVariety("Curry Leaf"),
            CropVariety("Black Pepper"),
            CropVariety("Long Pepper"),
            CropVariety("Cardamom", listOf("Green", "Black")),
            CropVariety("Cinnamon", listOf("Ceylon (True)", "Cassia")),
            CropVariety("Clove"),
            CropVariety("Nutmeg / Mace"),
            CropVariety("Star Anise"),
            CropVariety("Cumin"),
            CropVariety("Caraway"),
            CropVariety("Fenugreek (Methi Seed)"),
            CropVariety("Ajwain (Carom)"),
            CropVariety("Asafoetida (Hing)"),
            CropVariety("Vanilla", listOf("Bourbon", "Tahitian", "Mexican")),
            CropVariety("Saffron"),
            CropVariety("Paprika"),
        )),

        CropCategory("Beverage & Cash Crops", listOf(
            CropVariety("Coffee", listOf("Arabica (Typica)", "Arabica (Bourbon)", "Arabica (Geisha)", "Arabica (SL28)", "Arabica (Caturra)", "Arabica (Catuai)", "Robusta", "Liberica")),
            CropVariety("Tea", listOf("Black (Assam)", "Black (Darjeeling)", "Black (Ceylon)", "Green (Sencha)", "Green (Matcha)", "Green (Longjing)", "White (Silver Needle)", "Oolong", "Pu-erh")),
            CropVariety("Sugarcane", listOf("Co-86032", "Co-0238", "CP 88-1762", "B 49119")),
            CropVariety("Cocoa", listOf("Criollo", "Forastero", "Trinitario", "Nacional")),
            CropVariety("Sugar Beet"),
            CropVariety("Tobacco", listOf("Virginia (Flue-cured)", "Burley", "Oriental", "Maryland")),
            CropVariety("Hops", listOf("Cascade", "Centennial", "Citra", "Mosaic", "Saaz", "Hallertau", "Fuggle")),
            CropVariety("Hemp (Industrial)", listOf("Fiber", "Grain (Seed)", "CBD")),
        )),

        CropCategory("Fiber Crops", listOf(
            CropVariety("Cotton", listOf("Upland (G. hirsutum)", "Pima/Egyptian (G. barbadense)", "Sea Island", "Bt Cotton")),
            CropVariety("Jute"),
            CropVariety("Flax (Linen)"),
            CropVariety("Hemp (Fiber)"),
            CropVariety("Ramie"),
            CropVariety("Sisal"),
            CropVariety("Abaca (Manila Hemp)"),
            CropVariety("Kenaf"),
        )),

        CropCategory("Fodder & Forage", listOf(
            CropVariety("Alfalfa (Lucerne)"),
            CropVariety("Clover", listOf("Red", "White", "Crimson", "Berseem", "Subterranean")),
            CropVariety("Ryegrass", listOf("Annual (Italian)", "Perennial")),
            CropVariety("Timothy"),
            CropVariety("Bermudagrass"),
            CropVariety("Tall Fescue"),
            CropVariety("Napier Grass (Elephant Grass)"),
            CropVariety("Guinea Grass"),
            CropVariety("Vetch", listOf("Common", "Hairy")),
            CropVariety("Forage Sorghum"),
            CropVariety("Forage Maize / Silage Corn"),
        )),

        CropCategory("Flowers (Commercial)", listOf(
            CropVariety("Rose", listOf("Hybrid Tea", "Floribunda", "Damask", "Bourbon", "Climber", "Miniature")),
            CropVariety("Marigold", listOf("African", "French", "Signet")),
            CropVariety("Chrysanthemum"),
            CropVariety("Gerbera"),
            CropVariety("Tulip"),
            CropVariety("Lily", listOf("Asiatic", "Oriental", "Tiger", "Easter")),
            CropVariety("Jasmine"),
            CropVariety("Lavender", listOf("English", "French", "Spanish")),
            CropVariety("Carnation"),
            CropVariety("Gladiolus"),
            CropVariety("Tuberose"),
            CropVariety("Orchid", listOf("Phalaenopsis", "Cymbidium", "Dendrobium", "Vanda")),
            CropVariety("Hibiscus"),
            CropVariety("Sunflower (Ornamental)"),
        )),

        CropCategory("Mushrooms", listOf(
            CropVariety("Button (Agaricus)", listOf("White", "Cremini", "Portobello")),
            CropVariety("Oyster", listOf("Pearl", "Pink", "Yellow (Golden)", "Blue", "King Oyster")),
            CropVariety("Shiitake"),
            CropVariety("Enoki"),
            CropVariety("Maitake (Hen of the Woods)"),
            CropVariety("Lion's Mane"),
            CropVariety("Reishi"),
            CropVariety("Paddy Straw Mushroom"),
        )),
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Flat search index
    // ──────────────────────────────────────────────────────────────────────────

    data class CropEntry(
        val cropName: String,
        val categoryName: String,
        val variety: String? = null,
    ) {
        /** What gets displayed when the user selects this entry. */
        val displayName: String get() = if (variety != null) "$cropName · $variety" else cropName

        /** Combined text used for substring matching. */
        val searchText: String get() =
            listOfNotNull(cropName, variety, categoryName).joinToString(" ").lowercase()
    }

    val searchIndex: List<CropEntry> by lazy {
        categories.flatMap { cat ->
            cat.crops.flatMap { crop ->
                // Crop-level entry (no variety selected, just "Rice" etc.)
                val base = CropEntry(crop.cropName, cat.name)
                val vars = crop.varieties.map { v -> CropEntry(crop.cropName, cat.name, v) }
                listOf(base) + vars
            }
        }
    }

    fun search(query: String): List<CropEntry> {
        if (query.isBlank()) return emptyList()
        val tokens = query.trim().lowercase().split("\\s+".toRegex())
        val q = query.trim().lowercase()
        return searchIndex
            .filter { entry -> tokens.all { t -> entry.searchText.contains(t) } }
            .sortedWith(
                compareBy(
                    { if (it.cropName.lowercase().startsWith(q)) 0 else if (it.variety?.lowercase()?.startsWith(q) == true) 1 else 2 },
                    { it.cropName },
                    { it.variety ?: "" },
                )
            )
            .take(120)
    }
}
