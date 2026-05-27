package com.legalmotion.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.graphics.pdf.PdfDocument
import android.print.pdf.PrintedPdfDocument
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.FileOutputStream
import java.io.OutputStream

private enum class InterstitialReason(val headline: String, val subtitle: String) {
    DOCUMENT("Sponsored draft placement", "Google ad area placeholder before generating document")
}

private data class CourtDocumentTemplate(
    val title: String,
    val seedGoal: String,
    val seedFacts: String,
    val intro: String,
    val legalTheory: String
)

private val courtTemplates = listOf(
    CourtDocumentTemplate(
        title = "General Motion",
        seedGoal = "Request tailored relief from the Court based on the procedural posture of the case.",
        seedFacts = "The present record shows a concrete procedural problem that materially prejudices the defense if not corrected before further proceedings.",
        intro = "COMES NOW the Defendant, by and through undersigned counsel, and respectfully moves this Honorable Court for appropriate relief.",
        legalTheory = "The Court retains both the authority and the obligation to enter practical relief where the current posture of the case undermines fairness, preparation, or orderly criminal process."
    ),
    CourtDocumentTemplate(
        title = "Motion to Suppress",
        seedGoal = "Suppress the stop, search, seizure, or statement because law enforcement acted unlawfully.",
        seedFacts = "Body camera, dispatch records, and witness accounts materially undermine the asserted basis for the stop and the State cannot show a lawful exception curing the violation.",
        intro = "COMES NOW the Defendant and moves this Court to suppress evidence obtained in violation of constitutional guarantees and settled criminal procedure.",
        legalTheory = "Evidence derived from an unlawful detention, search, interrogation, or seizure is inadmissible and must be excluded to vindicate constitutional protections and deter official overreach."
    ),
    CourtDocumentTemplate(
        title = "Motion to Dismiss",
        seedGoal = "Dismiss the charging instrument or pending case because the State has failed to meet a threshold legal requirement.",
        seedFacts = "The charging allegations are materially deficient, fail to provide adequate notice, or otherwise cannot sustain the prosecution as a matter of law.",
        intro = "COMES NOW the Defendant and respectfully moves for dismissal of the pending charge or charging instrument.",
        legalTheory = "Where a prosecution is not legally sustainable on its face or in light of governing procedure, dismissal is the appropriate remedy rather than forcing the defense to litigate a defective case."
    ),
    CourtDocumentTemplate(
        title = "Motion to Compel Discovery",
        seedGoal = "Compel disclosure of evidence, reports, recordings, impeachment material, or other discovery the defense is entitled to receive.",
        seedFacts = "Despite repeated request, the State has not produced material information necessary for defense investigation, impeachment preparation, and trial readiness.",
        intro = "COMES NOW the Defendant and respectfully moves this Court for an order compelling immediate and complete discovery compliance.",
        legalTheory = "Meaningful defense preparation depends on timely disclosure of discoverable material, and the Court may compel production where delay, omission, or piecemeal compliance impairs fairness."
    ),
    CourtDocumentTemplate(
        title = "Motion for Continuance",
        seedGoal = "Continue the hearing or trial setting so the defense has adequate time to prepare.",
        seedFacts = "Critical discovery remains outstanding, witness preparation is incomplete, and forcing the matter forward on the current schedule would materially prejudice the defense.",
        intro = "COMES NOW the Defendant and respectfully moves for a continuance of the presently scheduled setting.",
        legalTheory = "A short continuance is warranted where additional time is reasonably necessary to protect effective assistance of counsel, review discovery, prepare witnesses, and avoid avoidable prejudice."
    ),
    CourtDocumentTemplate(
        title = "Motion in Limine",
        seedGoal = "Exclude inflammatory, irrelevant, or unfairly prejudicial material before it is mentioned before the finder of fact.",
        seedFacts = "The State is positioned to reference material that carries outsized prejudicial effect and little legitimate probative value, creating a substantial risk of unfair taint.",
        intro = "COMES NOW the Defendant and respectfully moves in limine for advance exclusion of specified matters before they are disclosed in the presence of the jury or other factfinder.",
        legalTheory = "Pretrial exclusion is appropriate where anticipated material is irrelevant, unduly prejudicial, cumulative, or likely to invite decision on an improper basis."
    ),
    CourtDocumentTemplate(
        title = "Motion to Modify Bond",
        seedGoal = "Modify bond conditions so they are reasonable, tailored, and no more restrictive than necessary.",
        seedFacts = "The existing bond conditions are disproportionate to the record, interfere with lawful employment or family obligations, and are not shown to be necessary to secure appearance or public safety.",
        intro = "COMES NOW the Defendant and respectfully moves for modification of the presently imposed bond conditions.",
        legalTheory = "Bond conditions must remain tethered to legitimate supervisory aims and may be modified where they are excessive, unsupported, or unnecessarily punitive in operation."
    ),
    CourtDocumentTemplate(
        title = "Request for Speedy Trial",
        seedGoal = "Set the matter for prompt trial and enforce the Defendant's right to a speedy disposition.",
        seedFacts = "The case has lingered without adequate justification, the defense is prepared to proceed, and continued delay materially burdens liberty, preparation, and witness availability.",
        intro = "COMES NOW the Defendant and formally demands a speedy trial and prompt trial setting in this cause.",
        legalTheory = "Where the defense is prepared and delay is no longer justified, the Court should protect the accused from further drift, uncertainty, and prejudice by setting the matter without unnecessary delay."
    ),
    CourtDocumentTemplate(
        title = "Motion to Withdraw Counsel",
        seedGoal = "Permit counsel to withdraw because continued representation has become impracticable, impaired, or inconsistent with professional obligations.",
        seedFacts = "A material breakdown has developed affecting communication, preparation, or representation in a manner that cannot be responsibly cured within the current attorney-client posture.",
        intro = "COMES NOW undersigned counsel and respectfully moves for leave of Court to withdraw from further representation in this matter.",
        legalTheory = "Where continued representation is no longer workable and withdrawal can be managed through orderly process, the Court may permit withdrawal while protecting the administration of justice."
    )
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LegalMotionScreen(
    darkModeEnabled: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onOpenDictionary: () -> Unit = {}
) {
    val context = LocalContext.current
    val isFreeVersion = remember(context) { context.packageName.endsWith(".free") }
    val clipboardManager = LocalClipboardManager.current
    var selectedTemplate by remember { mutableStateOf(courtTemplates.first()) }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var motionGoal by remember { mutableStateOf(courtTemplates.first().seedGoal) }
    var supportingFacts by remember { mutableStateOf(courtTemplates.first().seedFacts) }
    var polishedDraft by remember {
        mutableStateOf(generateCourtDraft(courtTemplates.first(), courtTemplates.first().seedGoal, courtTemplates.first().seedFacts))
    }
    var pendingDraft by remember { mutableStateOf<String?>(null) }
    var activeInterstitial by remember { mutableStateOf<InterstitialReason?>(null) }
    var countdownSeconds by remember { mutableStateOf(30) }
    var isEditingDraft by remember { mutableStateOf(false) }

    var filerName by rememberSaveable { mutableStateOf("") }
    var filerAddress by rememberSaveable { mutableStateOf("") }
    var filerCityStateZip by rememberSaveable { mutableStateOf("") }
    var filerPhone by rememberSaveable { mutableStateOf("") }
    var filerEmail by rememberSaveable { mutableStateOf("") }
    var filerRole by rememberSaveable { mutableStateOf("Pro Se") }
    var courtName by rememberSaveable { mutableStateOf("") }
    var countyDistrict by rememberSaveable { mutableStateOf("") }
    var plaintiffName by rememberSaveable { mutableStateOf("") }
    var caseNumber by rememberSaveable { mutableStateOf("") }
    var judgeName by rememberSaveable { mutableStateOf("") }
    var defendantName by rememberSaveable { mutableStateOf("") }
    var attorneyName by rememberSaveable { mutableStateOf("") }
    var barNumber by rememberSaveable { mutableStateOf("") }

    val saveDraftLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(polishedDraft)
            }
        }.onSuccess {
            Toast.makeText(context, "Draft saved successfully", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Unable to save draft", Toast.LENGTH_SHORT).show()
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                saveDraftAsPdf(selectedTemplate.title, polishedDraft, outputStream)
            }
        }.onSuccess {
            Toast.makeText(context, "PDF saved", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Unable to save PDF", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(activeInterstitial, countdownSeconds) {
        if (activeInterstitial == null) return@LaunchedEffect
        if (countdownSeconds > 0) {
            delay(1000)
            countdownSeconds -= 1
        } else {
            if (pendingDraft != null) {
                polishedDraft = pendingDraft.orEmpty()
                pendingDraft = null
            }
            activeInterstitial = null
            countdownSeconds = 30
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("legal-motion", fontWeight = FontWeight.Bold)
                        Text(
                            "Drafting desk",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenDictionary() }) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Courtroom Dictionary")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    launchExternalUrl(
                        context,
                        "https://www.paypal.com/donate/?business=kaynikko88%40gmail.com&currency_code=USD"
                    )
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Donate")
                }
            }
        },
        bottomBar = {
            if (isFreeVersion) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    AdBannerPlaceholder()
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isFreeVersion) {
                    AdBannerPlaceholder()
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Court document studio",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Translate raw case notes into polished courtroom prose.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Choose from the most common filings, enter the relief you want, then generate a formal draft in a seasoned defense-lawyer voice.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccentTag("9 document types")
                            AccentTag("Defense-counsel voice")
                            AccentTag("Save, copy, print")
                            AccentTag(if (darkModeEnabled) "Dark mode" else "Light mode")
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Filer information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Appears at the top of every generated filing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = filerName,
                            onValueChange = { filerName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Full name") },
                            placeholder = { Text("[YOUR NAME]") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = filerAddress,
                            onValueChange = { filerAddress = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Street address") },
                            placeholder = { Text("[YOUR ADDRESS]") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = filerCityStateZip,
                            onValueChange = { filerCityStateZip = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("City, State ZIP") },
                            placeholder = { Text("[CITY, STATE ZIP]") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = filerPhone,
                                onValueChange = { filerPhone = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Phone") },
                                placeholder = { Text("[PHONE]") },
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            OutlinedTextField(
                                value = filerEmail,
                                onValueChange = { filerEmail = it },
                                modifier = Modifier.weight(1.5f),
                                label = { Text("Email") },
                                placeholder = { Text("[EMAIL]") },
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                        }
                        Text(
                            text = "Appearing as",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Pro Se", "Attorney for Defendant").forEach { role ->
                                FilterChip(
                                    selected = filerRole == role,
                                    onClick = { filerRole = role },
                                    label = { Text(role) }
                                )
                            }
                        }
                        if (filerRole == "Attorney for Defendant") {
                            OutlinedTextField(
                                value = attorneyName,
                                onValueChange = { attorneyName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Attorney name") },
                                placeholder = { Text("e.g. Jane Smith, Esq.") },
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = barNumber,
                                onValueChange = { barNumber = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Bar number") },
                                placeholder = { Text("e.g. CA 123456") },
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Text(
                            text = "Court details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = courtName,
                            onValueChange = { courtName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Name of court") },
                            placeholder = { Text("e.g. Superior Court of California") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = countyDistrict,
                            onValueChange = { countyDistrict = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("County / District / Circuit") },
                            placeholder = { Text("e.g. County of Los Angeles") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        Text(
                            text = "Case parties",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = plaintiffName,
                            onValueChange = { plaintiffName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Plaintiff name") },
                            placeholder = { Text("[PLAINTIFF]") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = defendantName,
                            onValueChange = { defendantName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Defendant name") },
                            placeholder = { Text("[DEFENDANT]") },
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = caseNumber,
                                onValueChange = { caseNumber = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Case number") },
                                placeholder = { Text("e.g. 2024-CR-00123") },
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = judgeName,
                                onValueChange = { judgeName = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Judge") },
                                placeholder = { Text("Hon. [LAST NAME]") },
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Document type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box {
                            OutlinedButton(
                                onClick = { templateMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(selectedTemplate.title, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "Top court filing preset",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = templateMenuExpanded,
                                onDismissRequest = { templateMenuExpanded = false }
                            ) {
                                courtTemplates.forEach { template ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(template.title, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    text = template.seedGoal,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedTemplate = template
                                            motionGoal = template.seedGoal
                                            supportingFacts = template.seedFacts
                                            templateMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = motionGoal,
                            onValueChange = { motionGoal = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("What do you want the Court to do?") },
                            minLines = 3,
                            shape = RoundedCornerShape(18.dp),
                            supportingText = {
                                Text("Example: dismiss the indictment, compel Brady material, suppress the stop")
                            }
                        )

                        OutlinedTextField(
                            value = supportingFacts,
                            onValueChange = { supportingFacts = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Supporting facts") },
                            minLines = 4,
                            shape = RoundedCornerShape(18.dp),
                            supportingText = {
                                Text("Include dates, officer actions, missing evidence, prejudice, discovery problems, or notice defects")
                            }
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val compiledString = generateCourtDraft(selectedTemplate, motionGoal, supportingFacts, filerName, filerAddress, filerCityStateZip, filerPhone, filerEmail, courtName, countyDistrict, plaintiffName, caseNumber, judgeName, defendantName, attorneyName, barNumber)
                                    if (isFreeVersion) {
                                        pendingDraft = compiledString
                                        activeInterstitial = InterstitialReason.DOCUMENT
                                        countdownSeconds = 30
                                    } else {
                                        polishedDraft = compiledString
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Generate document")
                            }
                            OutlinedButton(
                                onClick = {
                                    motionGoal = ""
                                    supportingFacts = ""
                                    polishedDraft = ""
                                    pendingDraft = null
                                }
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Draft output",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Formatted in courtroom language with a senior-defense-counsel tone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    RoundedCornerShape(22.dp)
                                ),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = selectedTemplate.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                if (isEditingDraft) {
                                    OutlinedTextField(
                                        value = polishedDraft,
                                        onValueChange = { polishedDraft = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        minLines = 12,
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = if (polishedDraft.isBlank()) "Your generated document will appear here after the ad placement finishes." else polishedDraft,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        // Big edit / done button above the action row
                        Button(
                            onClick = { isEditingDraft = !isEditingDraft },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = polishedDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditingDraft)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = if (isEditingDraft) Icons.Default.Save else Icons.Default.Edit,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isEditingDraft) "Done editing" else "Edit document",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(polishedDraft))
                                    Toast.makeText(context, "Output copied", Toast.LENGTH_SHORT).show()
                                },
                                enabled = polishedDraft.isNotBlank()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy")
                            }
                            OutlinedButton(
                                onClick = { saveDraftLauncher.launch("legal-motion-${selectedTemplate.title.lowercase().replace(' ', '-')}.txt") },
                                enabled = polishedDraft.isNotBlank()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save .txt")
                            }
                            OutlinedButton(
                                onClick = { savePdfLauncher.launch("legal-motion-${selectedTemplate.title.lowercase().replace(' ', '-')}.pdf") },
                                enabled = polishedDraft.isNotBlank()
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save PDF")
                            }
                            OutlinedButton(
                                onClick = { printDraft(context, selectedTemplate.title, polishedDraft) },
                                enabled = polishedDraft.isNotBlank()
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Print")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(88.dp))
            }

            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    confirmButton = {
                        Button(onClick = { showSettingsDialog = false }) {
                            Text("Done")
                        }
                    },
                    title = { Text("Settings") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dark mode", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "Toggle between light and dark drafting views.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = darkModeEnabled,
                                    onCheckedChange = onDarkModeChange
                                )
                            }
                            Divider()
                            Text("Printer Setup", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Android print services discover Wi-Fi and Bluetooth printers automatically. Install a printer plugin (Mopria, HP, Canon, etc.) to connect your printer, then use the Print button.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Settings.ACTION_PRINT_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Print Settings")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val marketIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://search?q=print+service+plugin&c=apps")
                                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        runCatching { context.startActivity(marketIntent) }.onFailure {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://play.google.com/store/search?q=print+service&c=apps")
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Find Printer App")
                                }
                            }
                            Divider()
                            Text("About", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Legal Motion helps you draft court filings quickly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            if (activeInterstitial != null) {
                InterstitialAdOverlay(
                    reason = activeInterstitial!!,
                    secondsRemaining = countdownSeconds
                )
            }
        }
    }
}

@Composable
private fun AccentTag(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun AdBannerPlaceholder(title: String, subtitle: String) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                // Official testing Banner ID is ca-app-pub-3940256099942544/6300978111
                setAdUnitId("ca-app-pub-3940256099942544/6300978111")
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
private fun InterstitialAdOverlay(reason: InterstitialReason, secondsRemaining: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Google ad placeholder",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = reason.headline,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = reason.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "This slot is ready for a live Google ad SDK integration.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Continuing in ${secondsRemaining}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun generateCourtDraft(
    template: CourtDocumentTemplate,
    motionGoal: String,
    supportingFacts: String,
    filerName: String = "",
    filerAddress: String = "",
    filerCityStateZip: String = "",
    filerPhone: String = "",
    filerEmail: String = "",
    courtName: String = "",
    countyDistrict: String = "",
    plaintiffName: String = "",
    caseNumber: String = "",
    judgeName: String = "",
    defendantName: String = "",
    attorneyName: String = "",
    barNumber: String = ""
): String {
    val cleanedGoal = legalizeText(motionGoal.trim().ifBlank { template.seedGoal })
    val cleanedFacts = supportingFacts.trim().ifBlank { template.seedFacts }

    val factSentences = cleanedFacts
        .split(Regex("(?<=[.!?])\\s+"))
        .map { legalizeText(it.trim()) }
        .filter { it.isNotBlank() }

    // ── Resolved values used throughout the document ─────────────────────────
    val resolvedName      = filerName.trim().ifBlank { "[YOUR NAME]" }
    val resolvedDefendant = defendantName.trim().ifBlank { "Defendant" }
    val resolvedPlaintiff = plaintiffName.trim().ifBlank { "[PLAINTIFF]" }
    val resolvedAttorney  = attorneyName.trim().takeIf { it.isNotBlank() }
    val resolvedBar       = barNumber.trim().takeIf { it.isNotBlank() }
    val signatoryName     = resolvedAttorney ?: resolvedName

    // Substitute real defendant / attorney names into the template intro paragraph
    val resolvedIntro = template.intro
        .replace("the Defendant,", "$resolvedDefendant,")
        .replace("the Defendant and", "$resolvedDefendant and")
        .replace("the Defendant.", "$resolvedDefendant.")
        .replace("COMES NOW the Defendant ", "COMES NOW $resolvedDefendant ")
        .replace("undersigned counsel", resolvedAttorney ?: "undersigned counsel")

    return buildString {
        // ── Filer / attorney header ───────────────────────────────────────────
        if (resolvedAttorney != null) {
            appendLine(resolvedAttorney)
            if (resolvedBar != null) appendLine("Bar No. $resolvedBar")
            appendLine("Attorney for $resolvedDefendant")
        } else {
            appendLine(resolvedName)
            appendLine("Pro Se Defendant")
        }
        appendLine(filerAddress.ifBlank { "[YOUR ADDRESS]" })
        appendLine(filerCityStateZip.ifBlank { "[CITY, STATE ZIP]" })
        appendLine(filerPhone.ifBlank { "[PHONE NUMBER]" })
        appendLine(filerEmail.ifBlank { "[EMAIL ADDRESS]" })
        appendLine()
        appendLine()
        // ── Caption ──────────────────────────────────────────────────────────
        appendLine("IN THE ${courtName.trim().uppercase().ifBlank { "[NAME OF COURT]" }}")
        appendLine(countyDistrict.trim().ifBlank { "[COUNTY / DISTRICT / CIRCUIT], [STATE]" })
        appendLine()
        appendLine("$resolvedPlaintiff, Plaintiff,               Case No. ${caseNumber.trim().ifBlank { "__________________" }}")
        appendLine("                                             Judge: ${judgeName.trim().ifBlank { "____________________" }}")
        appendLine("v.")
        appendLine()
        appendLine("$resolvedDefendant, Defendant.")
        appendLine()
        appendLine()
        // ── Motion title ─────────────────────────────────────────────────────
        appendLine("${resolvedDefendant.uppercase()}'S ${template.title.uppercase()}")
        appendLine()
        appendLine()
        // ── Intro paragraph ──────────────────────────────────────────────────
        appendLine(resolvedIntro)
        appendLine()
        appendLine()
        // ── I. INTRODUCTION ──────────────────────────────────────────────────
        appendLine("I. INTRODUCTION")
        appendLine()
        appendLine(cleanedGoal.trimEnd('.').plus("."))
        appendLine()
        appendLine()
        // ── II. STATEMENT OF FACTS ───────────────────────────────────────────
        appendLine("II. STATEMENT OF FACTS")
        appendLine()
        factSentences.forEachIndexed { index, fact ->
            appendLine("    ${index + 1}. ${fact.trimEnd('.').plus(".")}")
        }
        appendLine()
        appendLine()
        // ── III. ARGUMENT ────────────────────────────────────────────────────
        appendLine("III. ARGUMENT")
        appendLine()
        appendLine("A. ${template.title}")
        appendLine()
        appendLine(template.legalTheory)
        appendLine()
        appendLine("The Court should not require $resolvedDefendant to proceed while these defects remain unresolved, particularly where the resulting prejudice is concrete, avoidable, and entirely foreseeable.")
        appendLine()
        appendLine()
        // ── IV. CONCLUSION ───────────────────────────────────────────────────
        appendLine("IV. CONCLUSION")
        appendLine()
        appendLine("WHEREFORE, $resolvedDefendant respectfully requests that this Court:")
        appendLine()
        appendLine("    1. Grant the ${template.title};")
        appendLine("    2. Award any further relief deemed just and proper; and")
        appendLine("    3. Grant such other relief as the Court considers appropriate.")
        appendLine()
        appendLine("Respectfully submitted,")
        appendLine()
        appendLine()
        // ── Signature block ──────────────────────────────────────────────────
        appendLine("________________________________")
        appendLine(signatoryName)
        if (resolvedAttorney != null) {
            appendLine("Attorney for $resolvedDefendant")
            if (resolvedBar != null) appendLine("Bar No. $resolvedBar")
        } else {
            appendLine("Pro Se Defendant")
        }
        appendLine(filerAddress.ifBlank { "[YOUR ADDRESS]" })
        appendLine(filerCityStateZip.ifBlank { "[CITY, STATE ZIP]" })
        appendLine(filerPhone.ifBlank { "[PHONE NUMBER]" })
        appendLine(filerEmail.ifBlank { "[EMAIL ADDRESS]" })
        appendLine("Date: ________________")
        appendLine()
        appendLine()
        // ── Certificate of Service ───────────────────────────────────────────
        appendLine("- - - - - - - - - - - - - - - - - - - - - - - - - - - -")
        appendLine()
        appendLine("CERTIFICATE OF SERVICE")
        appendLine()
        appendLine("I, $signatoryName, certify that on this ___ day of __________, 20__,")
        appendLine("I served a copy of this ${template.title} upon:")
        appendLine()
        appendLine("[NAME OF OPPOSING PARTY OR ATTORNEY]")
        appendLine("[ADDRESS]")
        appendLine()
        appendLine("By:  [ ] Mail   [ ] Email   [ ] Hand Delivery   [ ] Electronic Filing")
        appendLine()
        appendLine()
        appendLine("________________________________")
        append(signatoryName)
    }
}

private fun launchExternalUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun printDraft(context: Context, title: String, body: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    printManager.print(
        title,
        DraftPrintAdapter(context, title, body),
        PrintAttributes.Builder().build()
    )
}

private class DraftPrintAdapter(
    private val context: Context,
    private val title: String,
    private val body: String
) : PrintDocumentAdapter() {
    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        pdfDocument = PrintedPdfDocument(context, newAttributes)
        val info = PrintDocumentInfo.Builder("legal-motion-draft.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out android.print.PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val document = pdfDocument ?: return
        var pageIndex = 0
        var page = document.startPage(pageIndex)
        var canvas = page.canvas
        val cW = canvas.width.toFloat()
        val cH = canvas.height.toFloat()

        val marginX = cW * 0.088f
        val marginTop = cH * 0.091f
        val marginBottom = cH * 0.068f
        val maxW = cW - marginX * 2f

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = cH / 72f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val lineH = textPaint.textSize * 1.6f
        var currentY = marginTop

        wrapText(body, textPaint, maxW).forEach { line ->
            if (currentY + lineH > cH - marginBottom) {
                document.finishPage(page)
                pageIndex++
                page = document.startPage(pageIndex)
                canvas = page.canvas
                currentY = marginTop
            }
            if (line.isNotBlank()) canvas.drawText(line, marginX, currentY, textPaint)
            currentY += lineH
        }

        document.finishPage(page)
        try {
            FileOutputStream(destination.fileDescriptor).use { outputStream ->
                document.writeTo(outputStream)
            }
            callback.onWriteFinished(arrayOf(android.print.PageRange(0, pageIndex)))
        } catch (_: Exception) {
            callback.onWriteFailed("Unable to print draft")
        } finally {
            document.close()
            pdfDocument = null
        }
    }
}

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val lines = mutableListOf<String>()
    val paragraphs = text.split("\n")

    paragraphs.forEach { paragraph ->
        if (paragraph.isBlank()) {
            lines += ""
        } else {
            var currentLine = ""
            paragraph.split(" ").forEach { word ->
                val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = candidate
                } else {
                    if (currentLine.isNotBlank()) lines += currentLine
                    currentLine = word
                }
            }
            if (currentLine.isNotBlank()) lines += currentLine
        }
    }

    return lines
}

private fun saveDraftAsPdf(@Suppress("UNUSED_PARAMETER") title: String, body: String, outputStream: OutputStream) {
    val document = PdfDocument()
    val pageWidth = 612   // Letter 8.5" × 72 pts/in
    val pageHeight = 792  // Letter 11" × 72 pts/in
    val marginX = 54f     // 0.75" left/right
    val marginTop = 72f   // 1" top
    val marginBottom = 54f
    val maxWidth = pageWidth - marginX * 2

    val bodyPaint = Paint().apply {
        isAntiAlias = true
        textSize = 10f
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }

    var pageNum = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas
    var currentY = marginTop

    for (line in wrapText(body, bodyPaint, maxWidth)) {
        val lineH = 14f
        if (currentY + lineH > pageHeight - marginBottom) {
            document.finishPage(page)
            pageNum++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = marginTop
        }
        if (line.isNotEmpty()) canvas.drawText(line, marginX, currentY, bodyPaint)
        currentY += lineH
    }

    document.finishPage(page)
    document.writeTo(outputStream)
    document.close()
}
