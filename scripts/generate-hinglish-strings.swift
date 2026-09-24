import Foundation

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let hindiURL = root.appendingPathComponent("app/src/main/res/values-hi/strings.xml")
let curatedURL = root.appendingPathComponent("app/src/main/res/values-b+hi+Latn/strings.xml")
let legacyURL = root.appendingPathComponent("app/src/main/res/values-b+hi+Latn/legacy_overrides.xml")
let outputURL = root.appendingPathComponent("app/src/main/res/values-b+hi+Latn/generated_strings.xml")

let source = try String(contentsOf: hindiURL, encoding: .utf8)
let curated = try String(contentsOf: curatedURL, encoding: .utf8)
let expression = try NSRegularExpression(
    pattern: #"<string\s+name="([^"]+)"[^>]*>(.*?)</string>"#,
    options: [.dotMatchesLineSeparators]
)

func entries(in xml: String) -> [(name: String, value: String)] {
    let range = NSRange(xml.startIndex..<xml.endIndex, in: xml)
    return expression.matches(in: xml, range: range).compactMap { match in
        guard let nameRange = Range(match.range(at: 1), in: xml),
              let valueRange = Range(match.range(at: 2), in: xml) else { return nil }
        return (String(xml[nameRange]), String(xml[valueRange]))
    }
}

let curatedNames = Set(entries(in: curated).map(\.name))
let fixes: [String: String] = [
    "aja": "aaj", "apa": "aap", "apaka": "aapka", "apaki": "aapki",
    "apake": "aapke", "apana": "apna", "apani": "apni", "apane": "apne",
    "aura": "aur", "agara": "agar", "karem": "karein", "karen": "karein",
    "nahim": "nahi", "cuno": "chuno", "cuneṁ": "chunein", "cunen": "chunein",
    "dekhem": "dekhein", "dekhen": "dekhein", "rakhem": "rakhein",
    "rakhen": "rakhein", "likhem": "likhein", "likhen": "likhein",
    "suru": "shuru", "satha": "saath", "lie": "liye", "kyom": "kyon",
    "yahām": "yahan", "yaham": "yahan", "koī": "koi", "bahuta": "bahut",
    "phira": "phir", "dina": "din", "mana": "mann", "pura": "poora",
    "vapasa": "wapas", "svagata": "swagat", "mahasusa": "feel",
    "laksya": "goal", "pahala": "pehla", "pahale": "pehle", "bana'o": "banao",
    "posta": "post", "seva": "save", "li'e": "liye", "ko'i": "koi",
    "mem": "mein", "eka": "ek", "para": "par", "kamenta": "comment",
    "seyara": "share", "riporta": "report", "bata": "baat",
    "pasavarda": "password", "karane": "karne", "karana": "karna",
    "kama": "kam", "isa": "is", "ina": "in", "ta'itala": "title",
    "riseta": "reset", "muda": "mood", "parha'i": "padhai", "ga'i": "gayi",
    "entri": "entry", "ceka": "check", "baija": "badge", "taka": "tak",
    "loda": "load", "kucha": "kuch", "phokasa": "focus", "mahafila": "Mehfil",
    "linka": "link", "la'ika": "like", "krpaya": "please", "ki'e": "kiye",
    "hu'e": "hue", "safara": "SAFAR", "logina": "login", "jarnala": "journal",
    "imela": "email", "hu'a": "hua", "cahate": "chahte", "radda": "cancel",
    "pra'ivesi": "privacy", "haphte": "hafte", "ciza": "cheez", "caita": "chat",
    "baki": "baaki", "badalava": "changes", "tumhem": "tumhein", "thora": "thoda",
    "taiyara": "ready", "strika": "streak", "saporta": "support",
    "sakate": "sakte", "philinga": "feeling", "teja": "tez", "kitana": "kitna",
    "vajaha": "wajah", "upara": "upar", "socane": "sochne", "vakta": "waqt",
    "dila": "dil", "khulakara": "khulkar", "kadama": "kadam", "roza": "roz",
    "jita": "jeet", "nota": "note", "laga": "lag", "khuda": "khud",
    // Common ICU transliteration artifacts. Keep these here so regeneration does not
    // reintroduce unreadable spellings into the conversational Hinglish locale.
    "aipa": "app", "aipsa": "apps", "akaunta": "account", "analoka": "unlock",
    "apadeta": "update", "avasyaka": "zaroori", "bada": "baad", "banda": "band",
    "bara": "baar", "bhatakava": "bhatkaav", "bhejem": "bhejein", "bloka": "block",
    "banaem": "banayein", "cahie": "chahiye", "cainala": "channel", "cala": "chala", "calaem": "chalayen", "calane": "chalne",
    "calata": "chalta", "calu": "chalu", "cune": "chune", "cunem": "chunein",
    "chorem": "chhodein", "deta": "data", "dhyana": "Dhyan",
    "dilita": "delete", "dikhaem": "dikhayein", "egzama": "exam", "edamina": "admin",
    "filtara": "filter", "haibita": "habit", "haibitsa": "habits", "hara": "har",
    "hataem": "hataayein", "hisaba": "hisaab", "istemala": "istemal", "jaem": "jaayein",
    "jaega": "jaayega", "jorem": "jodein", "kaba": "kab", "karata": "karta",
    "kavaca": "Kavach", "kevala": "sirf", "kholem": "kholein", "khojem": "khojein",
    "kosisa": "koshish", "kula": "kul", "kvika": "quick", "laiva": "live",
    "lem": "lein", "minata": "minute", "moda": "mode", "nae": "naye", "nama": "naam",
    "nice": "neeche", "notiphikesana": "notification", "parhem": "padhein", "plana": "plan",
    "progresa": "progress", "rakhata": "rakhta", "saba": "sab", "samaya": "samay",
    "sesana": "session", "silda": "shield", "stadi": "study", "taimara": "timer",
    "taipa": "tap", "taya": "tay", "topiksa": "topics", "vidiyo": "video", "yaha": "yeh",
    "badalem": "badlein", "sakata": "sakta", "sirpha": "sirf", "apako": "aapko",
    "kauna": "kaun", "isase": "isse", "dekhane": "dekhne", "agala": "agla",
    "pichala": "pichhla", "stetasa": "status", "notifikesana": "notification",
    "meditesana": "meditation", "deli": "daily", "hama": "hum", "pasa": "paas",
    "sesansa": "sessions", "rikvesta": "request", "topika": "topic", "silebasa": "syllabus",
    "eksesa": "access", "eksaporta": "export", "mainyuali": "manually", "kastama": "custom",
    "peja": "page", "caiptara": "chapter", "sabjekta": "subject", "sabjektsa": "subjects",
    "teknikala": "technical", "dikkata": "dikkat", "saina": "sign", "yuseja": "usage",
    "setingsa": "settings", "studenta": "student", "tu-du": "to-do", "tarikhem": "dates", "vaha": "woh",
    "usaka": "uska", "samasya": "problem", "taki": "taaki", "thori": "thodi",
    "puri": "poori", "vale": "waale", "bare": "baare", "dera": "der", "chuta": "chhoot",
    "saphara": "Safar", "klasesa": "classes", "klasa": "class", "lautem": "lautein",
    "notiphikesansa": "notifications", "apadetsa": "updates", "somavara": "Somvaar",
    "rata": "raat", "asamana": "aasmaan", "muva": "move", "muskila": "mushkil",
    "sikhem": "seekhein", "matiriyala": "Material", "autalaina": "outline", "batana": "button",
    "marksa": "marks", "mayane": "maayne", "rakhate": "rakhte", "lekina": "lekin",
    "primiyama": "Premium", "daurana": "dauraan", "ona": "on", "una": "un",
    "bhatakate": "bhatkate", "usa": "us", "niyantrana": "niyantran", "stepa": "step",
    "skrina": "screen", "ektiva": "active", "thore": "thode", "samapta": "samaapt",
    "sandesa": "sandesh", "teksta": "text", "parhata": "padhta",
    "pasanda": "pasand", "dekhata": "dekhta", "alarta": "alert", "alartsa": "alerts",
    "basa": "bas", "asana": "aasaan", "raksa": "raksha", "kendrita": "kendrit",
    "divaisa": "device", "dhala": "dhaal", "taraha": "tarah", "vyaktigata": "vyaktigat",
    "janakari": "jaankari", "parhate": "padhte", "svikrta": "sweekrit",
    "konfigaresana": "configuration", "saransa": "saaransh", "prayasa": "prayaas",
    "manasika": "maansik", "svasthya": "swasthya", "adhyayana": "adhyayan",
    "utpadakata": "utpaadakta", "kalyana": "kalyaan", "samudaya": "samudaay",
    "lagatara": "lagataar", "sakriyata": "sakriyataa", "tivrata": "teevrata",
    "masika": "maasik", "snaipasota": "snapshot", "nirantarata": "nirantarta",
    "purnata": "poornata", "dara": "dar", "gaharai": "gehraai", "pravaha": "pravaah",
    "planara": "planner", "navinatama": "latest", "yutyuba": "YouTube", "phicarsa": "features",
    "vislesana": "vishleshan", "rikorda": "record", "khusa": "khush", "santa": "shaant",
    "udasa": "udaas", "cintita": "chintit", "utsahita": "utsaahit", "prerita": "prerit",
    "sirsaka": "sheershak", "vicara": "vichaar", "parhana": "padhna", "likhana": "likhna",
    "sima": "seema", "itihasa": "itihaas", "laksyom": "lakshyon", "prabandhita": "manage",
    "pragati": "progress", "traika": "track", "sampanna": "poora", "vivarana": "details",
    "vaikalpika": "optional", "prathamikata": "priority", "saptahika": "weekly",
    "palsa": "pulse", "dainika": "daily", "byora": "byora", "krama": "streak",
    "sabase": "sabse", "josa": "josh", "banae": "banaye", "adatem": "aadatein",
    "ausata": "ausat", "skora": "score", "trenda": "trend", "kausala": "kaushal",
    "radara": "radar", "bahuayami": "bahuaayami", "pradarsana": "performance",
    "pavara": "power", "ovara": "hour", "atma": "aatma", "khoja": "khoj",
    "mangalavara": "Mangalvaar", "budhavara": "Budhvaar", "guruvara": "Guruvaar",
    "sukravara": "Shukravaar", "sanivara": "Shanivaar", "ravivara": "Ravivaar",
    "sagara": "saagar", "registana": "registaan", "sadharana": "saadhaaran",
    "barisa": "baarish", "avaza": "awaaz", "bainorala": "binaural", "bitsa": "beats",
    "phai": "fi", "sangita": "sangeet", "jangala": "jungle", "vatavarana": "mahaul",
    "mauna": "silence", "garabara": "gadbad", "laita": "light",
    "barhem": "badhein", "barhaem": "badhaayein", "kariba": "kareeb",
    "fokasa": "focus", "samsa": "samay", "paem": "paayein", "aem": "aayein",
    "pichale": "pichhle", "maiseja": "message", "prophaila": "profile",
    "kanfarma": "confirm", "arkaiva": "archive", "inasaitsa": "insights",
    "rivizana": "revision", "sabsakripsana": "subscription", "taiga": "tag",
    "taima": "time", "logaauta": "logout", "rimaindara": "reminder",
    "sedyula": "schedule", "embienta": "ambient", "golsa": "goals",
    "chipaem": "chhipaayein", "jurem": "judein", "jorane": "jodne",
    "karate": "karte", "karake": "karke", "kholane": "kholne",
    "khulane": "khulne", "rahane": "rehne", "rahate": "rehte",
    "lagaem": "lagaayein", "dalem": "daalein", "doharaem": "dohraayein",
    "janem": "jaanein", "dosta": "dost", "jinhem": "jinhein",
    "chute": "chhoote", "chota": "chhota", "chote": "chhote",
    "hamesa": "hamesha", "hom": "ho", "jaegi": "jaayegi",
    "jaenge": "jaayenge", "huim": "hui",
    "samila": "shaamil", "sirfa": "sirf", "thika": "theek",
    "loga": "log", "jari": "jaari", "sala": "saal",
    "kara": "kar", "gae": "gaye", "rahem": "rahein", "aba": "ab",
    "ane": "aane", "calate": "chalte", "cale": "chale", "calie": "chaliye",
    "calem": "chalein", "maim": "main",
    "pitha": "peeth", "sabasi": "shaabaashi", "unhem": "unhein", "samajhem": "samjhein",
    "parhakara": "padhkar", "dhundhati": "dhoondhti",
    "turanta": "turant", "milati": "milti", "jisa": "jis",
    "jabase": "jabse", "jaba": "jab", "cahem": "chahein", "sakati": "sakti",
    "behatar": "behtar", "behatara": "behtar", "choti": "chhoti",
    "piche": "peeche", "jaemge": "jaayenge", "jaemgi": "jaayengi",
    "jaemga": "jaayega", "bica": "beech", "rupa": "roop", "age": "aage",
    "jaham": "jahan", "usaki": "uski", "jisase": "jisse",
    "dekhakara": "dekhkar", "inhem": "inhein", "pate": "paate",
    "pata": "pata", "kaica": "kaam", "rahiye": "rahiye", "rahie": "rahiye",
    "taska": "tasks", "vevsa": "waves", "bhula": "bhool",
    "jora": "joda", "cuna": "chuna",
    "dinom": "dinon", "kisa": "kis", "pae": "paaye",
    "dikhane": "dikhne", "bace": "bache",
    "barhiya": "badhiya", "mila": "mila",
    "bana": "bana", "banane": "banane",
    "seta": "set", "edita": "edit",
    "gola": "goal", "kamyuniti": "community", "kansistensi": "consistency",
    "darka": "dark", "dipa": "deep",
    "tabala": "table", "thima": "theme", "ranga": "rang",
    "madada": "madad", "javaba": "javaab", "subaha": "subah",
    "halki": "halki", "ninda": "neend",
    "isalie": "isliye", "yahim": "yahin",
    "pahacanata": "pehchaanta", "dostom": "doston",
    "sakem": "sakein", "sira": "sir",
    "sare": "saare", "vala": "waala", "gahari": "gehri", "haphta": "hafta",
    "naura": "nau", "nistha": "Nishtha", "parhane": "padhne", "samparka": "sampark",
    "jagaha": "jagah", "mahina": "mahina", "banate": "banate", "barabara": "barabar",
    "barhane": "badhaane", "badala": "badla", "bahara": "baahar", "chipata": "chhipata",
    "kitane": "kitne", "kitani": "kitni", "jata": "jaata", "kala": "kal",
    "paya": "paaya", "sitara": "sitaara", "dusari": "doosri", "dusare": "doosre",
    "khali": "khaali", "lagu": "laagu", "sakti": "sakti", "sakriya": "active",
    "mukhya": "main", "vyakti": "person", "suraksita": "safe",
    "aksara": "characters", "alaga": "alag", "arama": "aaraam", "baca": "bacha",
    "bacava": "bachaav", "besta": "best", "bhejane": "bhejne", "bolem": "bolein",
    "chorakara": "chhodkar", "cunane": "chunne", "donom": "dono",
    "edresa": "address", "ektiviti": "activity", "fidabaika": "feedback",
    "frikvensiza": "frequencies", "grupa": "group", "hala": "haal",
    "hamara": "hamara", "hitamaipa": "heatmap", "intazara": "intezaar",
    "jendara": "gender", "joina": "join",
    "kainsala": "Cancel", "kaitegari": "category", "kamentsa": "comments",
    "kamplisana": "completion", "kaneksana": "connection",
    "kholakara": "kholkar", "kopi": "copy", "laem": "laayein",
    "lista": "list", "maineja": "manage", "menyu": "menu",
    "normala": "normal", "ovaravyu": "overview", "paitarna": "pattern",
    "paraformensa": "performance", "piriyada": "period", "privyu": "preview",
    "prodaktiva": "productive", "prompta": "prompt", "proteksana": "protection",
    "renja": "range", "rifresa": "refresh", "riplai": "reply",
    "rokata": "rokta", "rokem": "rokein", "sapha": "saaf",
    "setaapa": "setup", "steja": "stage", "suruata": "shuruaat",
    "tarageta": "target", "tarikha": "tareekh", "testa": "test",
    "thambanela": "thumbnail", "utana": "utna", "santi": "shaanti",
    "acivamentsa": "achievements", "anaunsamentsa": "announcements",
    "bailensa": "balance", "bandala": "bundle", "baulsa": "bowls",
    "belsa": "bells", "besa": "bass", "cainalsa": "channels",
    "eksapayara": "expire", "enalitiksa": "analytics", "grupsa": "groups",
    "kailendara": "calendar", "kampozara": "composer", "klasikala": "classical",
    "korsa": "course", "korsesa": "courses", "maitriksa": "matrix",
    "materiyala": "material", "mentenensa": "maintenance", "metriksa": "metrics",
    "necara": "nature", "ordara": "order", "paidsa": "pads",
    "rikordingsa": "recordings", "rimaindarsa": "reminders",
    "sarvara": "server", "sarvisa": "service", "sinthsa": "synths",
    "taigarsa": "tigers", "ticara": "teacher", "traikara": "tracker",
    "yuzara": "user",
    "khatma": "khatam", "lagabhaga": "lagbhag", "cetavani": "warning",
    "pratisata": "percent", "sayada": "shayad", "hokara": "hokar",
]

func romanize(_ input: String) -> String {
    let latin = input.applyingTransform(.toLatin, reverse: false) ?? input
    var value = latin.folding(options: [.diacriticInsensitive], locale: Locale(identifier: "en"))
    value = value.replacingOccurrences(of: "।", with: ".")
    value = value.split(separator: " ", omittingEmptySubsequences: false).map { token in
        let raw = String(token)
        let prefix = raw.prefix { !$0.isLetter }
        let suffix = raw.reversed().prefix { !$0.isLetter }.reversed()
        let start = raw.index(raw.startIndex, offsetBy: prefix.count)
        let end = raw.index(raw.endIndex, offsetBy: -suffix.count)
        guard start <= end else { return raw }
        let core = String(raw[start..<end])
        return String(prefix) + (fixes[core.lowercased()] ?? core) + String(suffix)
    }.joined(separator: " ")
    return value.replacingOccurrences(of: "'", with: "")
}

// Sentences whose meaning or grammar cannot be fixed through single-word substitution.
let copyOverrides: [String: String] = [
    "maintenance_data_safe_title": "Your Data Is Safe",
    "nishtha_analytics_home": "Analytics Home",
    "nishtha_overview_subtitle": "Goals, Ekagra aur monthly review ka quick overview.",
    "nishtha_prompt_swipe_hint": "Upar swipe karein ya + par tap karein.",
    "ekagra_theme_forest": "Forest",
    "ekagra_theme_ocean": "Ocean",
    "ekagra_theme_desert": "Desert",
    "ekagra_theme_night": "Night Sky",
    "ekagra_theme_minimal": "Minimal",
    "ekagra_song_rain": "Rain Sounds",
    "ekagra_song_lofi": "Lo-Fi Music",
    "ekagra_stopwatch_running": "Stopwatch chal raha hai",
    "ekagra_break_running": "Break chal raha hai",
    "ekagra_pomodoro_running": "Pomodoro chal raha hai",
    "ekagra_running": "Ekagra chal raha hai",
    "ekagra_ready": "Focus start karein",
    "ekagra_take_break_buddy": "Ek Ekagra session 18 ghante tak chal sakta hai.",
    "ekagra_audio_playing": "Background audio chal raha hai",
    "ekagra_start_pomodoro": "Start Pomodoro",
    "ekagra_your_focus_sessions": "Focus Sessions",
    "ekagra_your_stopwatch_runs": "Stopwatch Runs",
    "ekagra_total_focus_time": "Total Focus Time",
    "ekagra_total_time": "Total Time",
    "ekagra_sessions": "Sessions",
    "ekagra_save_session": "Save Session",
    "ekagra_presence_ranked_body": "Leaderboard time jaari rakhne ke liye 30 minute ke andar Yes dabayein. Timer chalta rahega.",
    "notifications_permission_body": "study goals, focus sessions, daily streak aur nai class ke reminders paayein. zaroori account updates bhi bhejenge, taaki kuch miss na ho!",
    "notifications_revise_one_body": "aaj %2$s ka %1$s revise karein.",
    "kavach_consent_how_body": "Kavach sirf Ekagra focus timer chalne ke dauraan kaam karta hai. Timer band hote hi Kavach apps ko block karna band kar deta hai.",
    "tour_ask_title": "Namaste, main Titli hoon",
    "tour_ask_body_primary": "Kya main aapko app dikhaun?",
    "tour_ask_body_secondary_suffix": " dikhaungi — aap ise kabhi bhi skip ya band kar sakte hain.",
    "checkin_how_feeling": "Aap kaisa feel kar rahe hain?",
    "home_welcome_message": "Yahan bas aap aur hum hain, aur consistent rehne ki ek chhoti si koshish.\\n\\nChhoti jeet saath celebrate karenge, aur mushkil dinon mein bhi saath rahenge.\\n\\nAapki peeth par ek virtual shabaashi. Ab smile karein.",
    "support_can_wait": "Main wait kar sakta/sakti hoon",
    "planner_behind_schedule_notification": "Aap schedule se peeche hain — aaj thoda kaam poora kar lein!",
    "planner_change_order_help": "Topics move karein ya kaam ko aasaan ya mushkil mark karna seekhein.",
    "common_yes": "Yes",
    "app_tagline": "Aapki mental well-being ki journey yahan se shuru hoti hai.",
    "splash_tagline": "Marks maayne rakhte hain, lekin aapka mann bhi.",
    "nav_admin_notifications": "Admin notifications",
    "kavach_hero_body": "Kavach sirf Ekagra focus timer ke dauraan chune hue apps ko block karta hai.",
    "kavach_enable_title": "Kavach use karein",
    "kavach_guide_title": "Quick guide",
    "kavach_block_beast_footer": "Thodi der ke liye app use karna ho, to Quick Unlock available hai.",
    "kavach_info_permissions_body": "App Check dekhta hai ki app kab khula. Kavach Alert aapko SAFAR par wapas laata hai. Notifications study timer ke updates dikhate hain.",
    "kavach_permissions_needed": "Kavach shuru karne ke liye zaroori permissions dein.",
    "kavach_consent_agree": "I agree — Kavach chalu karein",
    "kavach_session_summary_subtitle": "Aapka Ekagra session poora ho gaya. Yeh raha aapka report card.",
    "dashboard_monthly_snapshot_help": "Is mahine ki performance par ek quick nazar.",
    "dashboard_weekly_mood_help": "Monday se Sunday tak aapka mood kaisa raha.",
    "dashboard_welcome_overlay_body": "Aapka SAFAR yahin se aage badhta hai.\\nHar chhota step maayne rakhta hai — aaj seekhne, sochne aur present rehne ka naya mauka hai.",
    "dashboard_awesome": "Shandaar!",
    "leaderboard_last_week_champions": "Pichhle hafte ke champions",
    "leaderboard_podium_pending": "Weekly cycle poora hone ke baad podium winners yahan dikhenge.",
    "support_load_error": "Aapki requests load nahi ho paayi.",
    "support_check_back_emergency": "Thodi der baad phir dekhein. Abhi khatre mein hain? Local emergency service ya paas ke kisi bharosemand insaan se sampark karein.",
    "auth_footer_tagline": "Kavach • har aspirant ki well-being ke liye",
]

let sourceEntries = entries(in: source).filter { !curatedNames.contains($0.name) }

// One-time migration: keep previously hand-edited generated copy, without calling it
// reviewed or allowing a future regeneration to silently replace it.
if CommandLine.arguments.contains("--adopt-existing") {
    if FileManager.default.fileExists(atPath: legacyURL.path) {
        fputs("legacy_overrides.xml already exists; refusing to overwrite it.\n", stderr)
        exit(1)
    }
    let expected = Dictionary(uniqueKeysWithValues: sourceEntries.map { ($0.name, copyOverrides[$0.name] ?? romanize($0.value)) })
    let existing = try String(contentsOf: outputURL, encoding: .utf8)
    let preserved = entries(in: existing).filter { expected[$0.name] != nil && expected[$0.name] != $0.value }
    let lines = preserved.map { "    <string name=\"\($0.name)\">\($0.value)</string>" }.joined(separator: "\n")
    let legacy = """
    <?xml version="1.0" encoding="utf-8"?>
    <!-- Existing manual Hinglish edits preserved during generator migration. These still need human review. -->
    <resources>
    \(lines)
    </resources>

    """
    try legacy.write(to: legacyURL, atomically: true, encoding: .utf8)
    print("Preserved \(preserved.count) existing Hinglish overrides in \(legacyURL.lastPathComponent).")
}

let legacy = FileManager.default.fileExists(atPath: legacyURL.path)
    ? try String(contentsOf: legacyURL, encoding: .utf8) : ""
let legacyNames = Set(entries(in: legacy).map(\.name))
let generated = sourceEntries
    .filter { !legacyNames.contains($0.name) }
    .map { "    <string name=\"\($0.name)\">\(copyOverrides[$0.name] ?? romanize($0.value))</string>" }
    .joined(separator: "\n")

let output = """
<?xml version="1.0" encoding="utf-8"?>
<!-- Generated from values-hi/strings.xml. Manual copy lives in strings.xml and legacy_overrides.xml. -->
<resources>
\(generated)
</resources>

"""
if CommandLine.arguments.contains("--check") {
    let existing = try String(contentsOf: outputURL, encoding: .utf8)
    guard existing == output else {
        let current = Dictionary(uniqueKeysWithValues: entries(in: existing).map { ($0.name, $0.value) })
        let expected = Dictionary(uniqueKeysWithValues: entries(in: output).map { ($0.name, $0.value) })
        let differing = Set(current.keys).union(expected.keys).filter { current[$0] != expected[$0] }.sorted()
        fputs("Hinglish generated_strings.xml differs for \(differing.count) keys: \(differing.prefix(20).joined(separator: ", ")).\n", stderr)
        for name in differing.prefix(5) {
            fputs("  \(name): current=\(current[name] ?? "<missing>") / generated=\(expected[name] ?? "<missing>")\n", stderr)
        }
        exit(1)
    }
} else {
    try output.write(to: outputURL, atomically: true, encoding: .utf8)
}
