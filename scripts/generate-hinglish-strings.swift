import Foundation

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let hindiURL = root.appendingPathComponent("app/src/main/res/values-hi/strings.xml")
let curatedURL = root.appendingPathComponent("app/src/main/res/values-b+hi+Latn/strings.xml")
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

let generated = entries(in: source)
    .filter { !curatedNames.contains($0.name) }
    .map { "    <string name=\"\($0.name)\">\(romanize($0.value))</string>" }
    .joined(separator: "\n")

let output = """
<?xml version="1.0" encoding="utf-8"?>
<!-- Generated from values-hi/strings.xml. Curated overrides live in strings.xml. -->
<resources>
\(generated)
</resources>

"""
try output.write(to: outputURL, atomically: true, encoding: .utf8)
