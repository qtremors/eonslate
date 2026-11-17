package dev.qtremors.eonslate.core.tracker.domain.finance

internal val paymentCategoryKeywords = mapOf(
    "Food & Dining" to listOf(
        "swiggy", "zomato", "starbucks", "mcdonald", "kfc", "burger king", "domino", "subway",
        "zepto", "blinkit", "instamart", "bigbasket", "bb daily", "dunkin", "pizza hut", "taco bell",
        "dunzo", "haldiram", "chai point", "blue tokai", "third wave", "barbeque nation", "behrouz",
        "faasos", "eatclub", "freshmenu", "ovenstory", "wendy", "wow momo", "rebel foods",
        "tim hortons", "baskin robbins", "costa coffee", "cafe coffee day", "nature basket",
        "licious", "country delight", "box8", "biryani by kilo", "milk basket", "grofers",
        "grocery", "restaurant", "cafe", "bakery", "supermarket", "dining", "food", "kitchen",
        "sweet", "sweets", "bakes", "dhaba", "bhojanalya", "eats", "bar", "pub", "brewery",
        "doordash", "uber eats", "grubhub", "deliveroo", "instacart"
    ),
    "Transport" to listOf(
        "uber", "ola", "rapido", "metro", "shell", "indian oil", "hpcl", "bpcl", "irctc",
        "makemytrip", "redbus", "abhibus", "yulu", "blusmart", "zingbus", "goibibo", "cleartrip",
        "indigo", "air india", "vistara", "spicejet", "akasa", "fuel", "petrol", "diesel",
        "cng", "fastag", "toll", "parking", "cab", "auto", "railway", "train", "flight", "airlines",
        "airways", "lyft", "grab", "fuel pump", "petroleum", "service station", "autoworks"
    ),
    "Shopping" to listOf(
        "amazon", "flipkart", "myntra", "zara", "h&m", "hm", "d-mart", "dmart", "reliance retail",
        "reliance trends", "reliance digital", "nykaa", "ajio", "tata cliq", "meesho", "urbanic",
        "uniqlo", "marks & spencer", "westside", "pantaloons", "lifestyle", "shoppers stop",
        "croma", "vijay sales", "decathlon", "ikea", "nike", "adidas", "puma", "sephora",
        "lenskart", "tanishq", "caratlane", "kalyan", "titan", "apple store", "retail", "mall",
        "store", "mart", "bazaar", "boutique", "apparel", "clothing", "jewel", "jewellers",
        "footwear", "opticals", "walmart", "target", "costco", "best buy", "ebay", "etsy"
    ),
    "Entertainment" to listOf(
        "netflix", "spotify", "apple", "google play", "youtube", "hotstar", "disney",
        "sonyliv", "bookmyshow", "steam", "playstation", "xbox", "nintendo", "prime video",
        "zee5", "jiocinema", "pvr", "inox", "cinepolis", "cinema", "theatre", "movie",
        "gaming", "discord", "twitch", "audible", "kindle", "hulu", "paramount", "peacock",
        "epic games", "crunchyroll", "tidal", "deezer", "soundcloud", "gaana", "wynk"
    ),
    "Utilities" to listOf(
        "electricity", "bescom", "adani", "airtel", "jio", "vi", "vodafone", "tata sky",
        "tata play", "hathway", "act fibernet", "water", "gas", "igl", "mgl", "mahanagar gas",
        "torrent power", "discom", "broadband", "wifi", "recharge", "postpaid", "prepaid",
        "dth", "utility", "bill payment", "municipal", "cesc", "tneb", "mseb", "uppcl",
        "bsnl", "mtnl", "pipeline", "sewage", "clean water", "power corp"
    ),
    "Health" to listOf(
        "apollo", "netmeds", "1mg", "tata 1mg", "practo", "hospital", "pharmacy", "medplus",
        "pharmeasy", "dr lal pathlabs", "srl diagnostics", "metropolis", "max healthcare",
        "fortis", "manipal", "cult.fit", "gym", "fitness", "dental", "clinic", "doctor",
        "medical", "medicine", "health", "care", "wellness", "therapist", "optometry",
        "diagnostic", "pathology", "cvs", "walgreens", "boots"
    ),
    "Subscriptions" to listOf(
        "subscription", "recurring", "auto-debit", "standing instruction", "mandate",
        "si txn", "e-mandate", "si on card", "nach", "ecs mandate"
    ),
    "Income" to listOf(
        "salary", "retainer", "payout", "dividend", "interest credited", "stipend",
        "allowance", "bonus", "commission", "employer", "payroll", "freelance",
        "rent received", "cashback credited"
    )
)
