@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.clubfirst.football

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

// A working, offline-first prototype. Club colours are preliminary and NOT verified brand assets.
data class Club(val id: String, val name: String, val league: String, val primary: Long, val secondary: Long, val official: String)
private val clubs = listOf(
    Club("lfc", "Liverpool", "Premier League", 0xFFDC0714, 0xFFF6EB61, "https://www.liverpoolfc.com"),
    Club("mufc", "Manchester United", "Premier League", 0xFFDA291C, 0xFFFBE122, "https://www.manutd.com"),
    Club("cfc", "Chelsea", "Premier League", 0xFF034694, 0xFFFFFFFF, "https://www.chelseafc.com"),
    Club("afc", "Arsenal", "Premier League", 0xFFEF0107, 0xFFFFFFFF, "https://www.arsenal.com"),
    Club("mcfc", "Manchester City", "Premier League", 0xFF6CABDD, 0xFF1C2C5B, "https://www.mancity.com"),
    Club("drfc", "Doncaster Rovers", "EFL League One", 0xFFDA291C, 0xFFFFFFFF, "https://www.doncasterroversfc.co.uk"),
    Club("rm", "Real Madrid", "La Liga", 0xFF594B98, 0xFFFFFFFF, "https://www.realmadrid.com"),
    Club("barca", "FC Barcelona", "La Liga", 0xFF004D98, 0xFFA50044, "https://www.fcbarcelona.com"),
    Club("bayern", "Bayern Munich", "Bundesliga", 0xFFDC052D, 0xFFFFFFFF, "https://fcbayern.com"),
    Club("psg", "Paris Saint-Germain", "Ligue 1", 0xFF004170, 0xFFDA291C, "https://www.psg.fr")
)
private val leagues = listOf("Premier League", "EFL Championship", "EFL League One", "EFL League Two", "FA Cup", "Champions League", "Europa League", "La Liga", "Bundesliga", "Serie A", "Ligue 1")
private val pages = listOf("Home", "Live", "Calendar", "Updates", "Clubs")
private val pageIcons = listOf(Icons.Default.Home, Icons.Default.Sensors, Icons.Default.CalendarMonth, Icons.Default.Newspaper, Icons.Default.Shield)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0
        setContent { FootballApp() }
    }
}

private fun openLink(ctx: android.content.Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun addToCalendar(ctx: android.content.Context, title: String) {
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, title)
    runCatching { ctx.startActivity(intent) }
}

@Composable
private fun FootballApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("football_preview", android.content.Context.MODE_PRIVATE) }
    val followed = remember { mutableStateListOf<String>().also { it.addAll(prefs.getString("clubs", "lfc")!!.split(",").filter(String::isNotBlank)) } }
    val followedLeagues = remember { mutableStateListOf<String>().also { it.addAll(prefs.getString("leagues", "Premier League")!!.split("|").filter(String::isNotBlank)) } }
    var selectedId by remember { mutableStateOf(prefs.getString("selected", "lfc") ?: "lfc") }
    var mode by remember { mutableStateOf("clubs") }
    var page by remember { mutableStateOf("Home") }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var showSettings by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarded", false)) }
    var section by remember { mutableStateOf("Overview") }
    val selected = clubs.find { it.id == selectedId } ?: clubs.first()
    val neutral = mode == "leagues" || selectedId == "all"
    val brand = if (neutral) Color(0xFF397E7D) else Color(selected.primary)
    val scheme = if (dark) darkColorScheme(primary = brand, secondary = if (neutral) Color(0xFF7FCAC4) else Color(selected.secondary), background = Color(0xFF10161C), surface = Color(0xFF1B2530), surfaceVariant = Color(0xFF283441), onPrimary = Color.White)
                 else lightColorScheme(primary = brand, secondary = Color(0xFF42566D), background = Color(0xFFF4F6F8), surface = Color.White, surfaceVariant = Color(0xFFE8EEF3))
    MaterialTheme(colorScheme = scheme, shapes = Shapes(medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp))) {
        if (showOnboarding) {
            SetupScreen(followed, followedLeagues, onDone = {
                prefs.edit().putBoolean("onboarded", true).putString("clubs", followed.joinToString(",")).putString("leagues", followedLeagues.joinToString("|")).apply()
                selectedId = followed.firstOrNull() ?: "all"
                showOnboarding = false
            })
        } else {
            Scaffold(
                topBar = {
                    Column {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (neutral) "YOUR FOOTBALL" else selected.name.uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
                                Text("Your clubs. Your game.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Settings") }
                        }
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            listOf("clubs" to "My Clubs", "leagues" to "My Leagues").forEachIndexed { index, pair ->
                                SegmentedButton(selected = mode == pair.first, onClick = { mode = pair.first }, shape = SegmentedButtonDefaults.itemShape(index, 2), label = { Text(pair.second) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (mode == "clubs") {
                            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selectedId == "all", onClick = { selectedId = "all"; prefs.edit().putString("selected", "all").apply() }, label = { Text("All Clubs") })
                                followed.mapNotNull { id -> clubs.find { it.id == id } }.forEach { club ->
                                    FilterChip(selectedId == club.id, onClick = { selectedId = club.id; prefs.edit().putString("selected", club.id).apply() }, label = { Text(club.name) })
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar {
                        pages.forEachIndexed { i, name ->
                            NavigationBarItem(selected = page == name, onClick = { page = name; section = "Overview" }, icon = { Icon(pageIcons[i], name) }, label = { Text(name) }, alwaysShowLabel = true)
                        }
                    }
                }
            ) { pad ->
                LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        when (page) {
                            "Home" -> HomeScreen(selected, neutral, mode, followed, followedLeagues, brand, { page = it }, { openLink(ctx, it) })
                            "Live" -> LiveScreen(selected, neutral, { openLink(ctx, it) })
                            "Calendar" -> CalendarScreen(prefs, { addToCalendar(ctx, it) })
                            "Updates" -> UpdatesScreen(selected, neutral, { openLink(ctx, it) })
                            "Clubs" -> ClubHub(selected, neutral, section, { section = it }, prefs, { openLink(ctx, it) })
                        }
                    }
                    item { Spacer(Modifier.height(18.dp)) }
                }
            }
            if (showSettings) AlertDialog(onDismissRequest = { showSettings = false }, title = { Text("App settings") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Dark appearance", Modifier.weight(1f)); Switch(dark, onCheckedChange = { dark = it; prefs.edit().putBoolean("dark", it).apply() }) }
                    Text("Followed clubs and leagues", fontWeight = FontWeight.SemiBold)
                    Text("Change your selection at any time.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { showSettings = false; showOnboarding = true }) { Text("Manage following") }
                    Text("Preview 0.1.3 • No live-data provider connected", style = MaterialTheme.typography.labelSmall)
                }
            }, confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Close") } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SetupScreen(followed: MutableList<String>, followedLeagues: MutableList<String>, onDone: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var leagueTab by remember { mutableStateOf(false) }
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Choose your football") }) }, bottomBar = {
        Button(onClick = onDone, enabled = followed.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Continue with ${followed.size} club${if (followed.size == 1) "" else "s"}") }
    }) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp)) {
            Text("Follow any of the starter clubs below. Your selection controls the theme and content. This is an expandable demo catalogue.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!leagueTab, onClick = { leagueTab = false }, label = { Text("Clubs") })
                FilterChip(leagueTab, onClick = { leagueTab = true }, label = { Text("Leagues") })
            }
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!leagueTab) items(clubs.filter { it.name.contains(query, true) || it.league.contains(query, true) }) { club ->
                    Row(Modifier.fillMaxWidth().clickable { if (followed.contains(club.id)) followed.remove(club.id) else followed.add(club.id) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).background(Color(club.primary), RoundedCornerShape(9.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(club.name); Text(club.league, style = MaterialTheme.typography.labelSmall) }
                        Checkbox(checked = followed.contains(club.id), onCheckedChange = { if (it) { if (!followed.contains(club.id)) followed.add(club.id) } else followed.remove(club.id) })
                    }
                } else items(leagues.filter { it.contains(query, true) }) { league ->
                    Row(Modifier.fillMaxWidth().clickable { if (followedLeagues.contains(league)) followedLeagues.remove(league) else followedLeagues.add(league) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(league, Modifier.weight(1f)); Checkbox(checked = followedLeagues.contains(league), onCheckedChange = { if (it) { if (!followedLeagues.contains(league)) followedLeagues.add(league) } else followedLeagues.remove(league) })
                    }
                }
            }
        }
    }
}

@Composable private fun CardPanel(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
@Composable private fun MiniAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp)) { Icon(icon, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(label, maxLines = 1) }
}
@Composable private fun DemoNotice() { Text("PREVIEW DATA ONLY · No live feed connected", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }

@Composable private fun HomeScreen(club: Club, neutral: Boolean, mode: String, followed: List<String>, followedLeagues: List<String>, brand: Color, navigate: (String) -> Unit, open: (String) -> Unit) {
    CardPanel(if (mode == "leagues") "Your leagues" else if (neutral) "All your clubs" else "${club.name} · Home", "A personalised view, not a general football feed") {
        if (mode == "leagues") {
            if (followedLeagues.isEmpty()) Text("No leagues selected. Add some in Settings.")
            followedLeagues.forEach { Text("• $it") }
        } else if (neutral) {
            followed.mapNotNull { id -> clubs.find { it.id == id } }.forEach { Text("• ${it.name}") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(brand, RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(8.dp)); Text("Club colour theme active")
            }
            Text("Fixture cards and news will populate when a licensed football-data feed is connected.")
            MiniAction("Official club site", Icons.Default.OpenInNew) { open(club.official) }
        }
    }
    CardPanel("Next fixture", "Fixtures are intentionally not fabricated") {
        DemoNotice()
        Text("Upcoming fixtures will appear here, including kick-off time, venue, competition and broadcaster.")
        Button(onClick = { navigate("Live") }) { Icon(Icons.Default.SportsSoccer, null); Spacer(Modifier.width(8.dp)); Text("Open Matchday Centre") }
    }
    CardPanel("Your daily briefing", "Reliable sources, tailored to your selections") {
        Text("Club announcements, team news, fixtures and confirmed transfers will be summarised once news feeds are connected.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniAction("BBC Sport", Icons.Default.Newspaper) { open("https://www.bbc.com/sport/football") }
            MiniAction("Sky Sports", Icons.Default.OpenInNew) { open("https://www.skysports.com/football") }
        }
    }
    CardPanel("Quick access") {
        MiniAction("Radio & TV", Icons.Default.Headphones) { navigate("Live") }
        MiniAction("Calendar", Icons.Default.CalendarMonth) { navigate("Calendar") }
        MiniAction("Squad & analysis", Icons.Default.Analytics) { navigate("Clubs") }
    }
}

@Composable private fun LiveScreen(club: Club, neutral: Boolean, open: (String) -> Unit) {
    CardPanel("Matchday Centre", "Before kick-off • Live • Full-time") {
        DemoNotice()
        Text("When fixtures are connected, this view will show the live score, match timeline, lineups, formations, substitutions, xG and verified physical-performance measurements.")
        HorizontalDivider()
        Text("Match state: Awaiting provider", fontWeight = FontWeight.SemiBold)
        Text("No match or player tracking data is currently available in this preview.")
    }
    CardPanel("Listen live", "Official and authorised streams only") {
        Text("Open a broadcaster to find available commentary. In-app background playback will require an authorised playable stream.")
        MiniAction("BBC Sounds", Icons.Default.Headphones) { open("https://www.bbc.co.uk/sounds") }
        MiniAction("talkSPORT", Icons.Default.Radio) { open("https://talksport.com") }
    }
    CardPanel("Where to watch", "UK television and streaming") {
        Text("Per-match channel and subscription details will appear when a broadcast-schedule data source is connected.")
        MiniAction("Sky Sports", Icons.Default.Tv) { open("https://www.skysports.com/football") }
        MiniAction("TNT Sports", Icons.Default.Tv) { open("https://www.tntsports.co.uk") }
        MiniAction("BBC Sport", Icons.Default.Tv) { open("https://www.bbc.com/sport/football") }
    }
    CardPanel("Physical performance", "Only measured, authorised metrics") {
        listOf("Top speed", "Distance covered", "Sprint count", "High-speed running", "Heatmaps & tracking").forEach { metric ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(metric); Text("Not available", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable private fun CalendarScreen(prefs: android.content.SharedPreferences, addCalendar: (String) -> Unit) {
    var eventTitle by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    val events = remember { mutableStateListOf<Pair<String, String>>() }
    LaunchedEffect(Unit) {
        events.clear()
        val arr = runCatching { JSONArray(prefs.getString("events", "[]")) }.getOrDefault(JSONArray())
        for (i in 0 until arr.length()) { val obj = arr.optJSONObject(i) ?: continue; events.add(obj.optString("date") to obj.optString("title")) }
    }
    CardPanel("Your football calendar", "Fixtures, ticket dates, club events and your own plans") {
        DemoNotice()
        Text("Live fixtures and postponements will synchronise after a football API is connected. You can already save personal events below.")
    }
    CardPanel("Personal events") {
        OutlinedTextField(eventTitle, { eventTitle = it }, label = { Text("Event name") }, placeholder = { Text("e.g. Away day with friends") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(eventDate, { eventDate = it }, label = { Text("Date / time") }, placeholder = { Text("e.g. 14 Nov, 15:00") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = {
            events.add(eventDate.trim() to eventTitle.trim())
            prefs.edit().putString("events", JSONArray().apply { events.forEach { put(JSONObject().put("date", it.first).put("title", it.second)) } }.toString()).apply()
            eventTitle = ""; eventDate = ""
        }, enabled = eventTitle.isNotBlank() && eventDate.isNotBlank()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Save event") }
        if (events.isEmpty()) Text("No personal events yet.")
        events.forEachIndexed { index, event ->
            HorizontalDivider()
            Text(event.first, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(event.second, fontWeight = FontWeight.SemiBold)
            Row {
                TextButton(onClick = { addCalendar("${event.second} (${event.first})") }) { Text("Add to device calendar") }
                TextButton(onClick = {
                    events.removeAt(index)
                    prefs.edit().putString("events", JSONArray().apply { events.forEach { put(JSONObject().put("date", it.first).put("title", it.second)) } }.toString()).apply()
                }) { Text("Delete") }
            }
        }
    }
}

@Composable private fun UpdatesScreen(club: Club, neutral: Boolean, open: (String) -> Unit) {
    var filter by remember { mutableStateOf("News") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
        listOf("News", "Socials", "Videos").forEach { FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it) }) }
    }
    when (filter) {
        "News" -> {
            CardPanel("Trusted football news", "Sources are linked, not scraped or reproduced") {
                Text("News will be filtered to your followed clubs when publisher-approved feeds are available.")
                MiniAction("BBC Sport", Icons.Default.OpenInNew) { open("https://www.bbc.com/sport/football") }
                MiniAction("Sky Sports", Icons.Default.OpenInNew) { open("https://www.skysports.com/football") }
                MiniAction("The Athletic", Icons.Default.OpenInNew) { open("https://www.nytimes.com/athletic/football/") }
                if (!neutral) MiniAction("${club.name} official", Icons.Default.Verified) { open(club.official) }
            }
        }
        "Socials" -> CardPanel("Official club & player socials", "Verified sources, not an unfiltered social feed") {
            Text("Official club and player posts will appear when supported by platform permissions. For now, use the club's official links to find verified accounts.")
            if (!neutral) MiniAction("Official club channels", Icons.Default.OpenInNew) { open(club.official) }
            Text("Planned filters: All · Clubs · Players · Instagram · X")
        }
        else -> CardPanel("Club TV & highlights", "Official videos only") {
            Text("Highlights, interviews and press conferences require authorised embeds or official links.")
            if (!neutral) MiniAction("${club.name} videos", Icons.Default.PlayCircle) { open(club.official) }
            MiniAction("YouTube", Icons.Default.OpenInNew) { open("https://www.youtube.com") }
        }
    }
}

@Composable private fun ClubHub(club: Club, neutral: Boolean, section: String, onSection: (String) -> Unit, prefs: android.content.SharedPreferences, open: (String) -> Unit) {
    if (neutral) {
        CardPanel("Choose a club", "Club-specific sections use each team's identity") { Text("Select an individual club using the chips at the top to see its squad, history, tickets, analysis and personal memories.") }
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Overview", "Analysis", "Squad", "History", "Stadium", "Memories").forEach { FilterChip(section == it, onClick = { onSection(it) }, label = { Text(it) }) }
    }
    when (section) {
        "Overview" -> {
            CardPanel(club.name, club.league) {
                Text("Club hub: fixtures, results, squad, statistics, tickets and supporter memories.")
                MiniAction("Official website", Icons.Default.OpenInNew) { open(club.official) }
            }
            CardPanel("Season tracker") {
                Text("League position, form, results, goals, clean sheets and competition progress will appear when a data provider is connected.")
                DemoNotice()
            }
        }
        "Analysis" -> {
            CardPanel("Advanced analysis", "Tactical and physical performance") {
                DemoNotice()
                listOf("Expected goals (xG)", "Shot maps", "Passing networks", "Player heatmaps", "Top speeds", "Sprint counts", "Distance covered", "Season trends").forEach {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(it); Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Text("Missing data is deliberately left blank, not estimated.", style = MaterialTheme.typography.bodySmall)
            }
        }
        "Squad" -> CardPanel("Squad & players") {
            Text("Player profiles, positions, appearances, minutes, goals, assists, injuries and tracking metrics will be populated by the football provider.")
            DemoNotice()
        }
        "History" -> CardPanel("Club history") {
            Text("Honours, historic seasons, major fixtures and all-time records will be added using verified historical sources.")
            MiniAction("Official club history", Icons.Default.History) { open(club.official) }
        }
        "Stadium" -> CardPanel("Tickets & stadium") {
            Text("Official ticket links, travel information, accessibility, stadium facilities and ticket-release reminders.")
            MiniAction("Official ticket information", Icons.Default.ConfirmationNumber) { open(club.official) }
        }
        "Memories" -> MemoriesScreen(club, prefs)
    }
}

@Composable private fun MemoriesScreen(club: Club, prefs: android.content.SharedPreferences) {
    val key = "memories_${club.id}"
    val memories = remember(club.id) { mutableStateListOf<String>().also { list -> val arr = runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrDefault(JSONArray()); for (i in 0 until arr.length()) list.add(arr.optString(i)) } }
    var text by remember(club.id) { mutableStateOf("") }
    CardPanel("My match memories", "Private notes saved on this device") {
        OutlinedTextField(text, { text = it }, label = { Text("Match, date and your memory") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(onClick = {
            memories.add(0, text.trim()); text = ""
            prefs.edit().putString(key, JSONArray().apply { memories.forEach { put(it) } }.toString()).apply()
        }, enabled = text.isNotBlank()) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Save memory") }
        if (memories.isEmpty()) Text("Your first match memory will appear here.")
        memories.forEachIndexed { i, memory ->
            HorizontalDivider(); Text(memory)
            TextButton(onClick = { memories.removeAt(i); prefs.edit().putString(key, JSONArray().apply { memories.forEach { put(it) } }.toString()).apply() }) { Text("Delete") }
        }
    }
}
