package com.legalmotion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

data class LegalEntry(
    val term: String,
    val category: String,
    val definition: String,
    val plainEnglish: String
)

// ---------------------------------------------------------------------------
// Courtroom term data
// ---------------------------------------------------------------------------

val courtroomTerms: List<LegalEntry> = listOf(
    LegalEntry(
        term = "Arraignment",
        category = "Criminal Procedure",
        definition = "A formal reading of a criminal charge in the presence of the defendant, to inform them of the charges against them and to allow them to enter a plea.",
        plainEnglish = "The first court appearance where the judge tells you exactly what you're charged with and asks how you plead — guilty, not guilty, or no contest."
    ),
    LegalEntry(
        term = "Hearsay",
        category = "Evidence",
        definition = "An out-of-court statement offered to prove the truth of the matter asserted. Generally inadmissible under the Federal Rules of Evidence unless a recognized exception applies.",
        plainEnglish = "Repeating what someone else said outside of court to prove that thing is true. Courts usually won't allow it — unless a specific legal exception applies."
    ),
    LegalEntry(
        term = "Pro Se",
        category = "Representation",
        definition = "A Latin term meaning 'for oneself.' Refers to a party in a legal proceeding who represents themselves without the assistance of an attorney.",
        plainEnglish = "Representing yourself in court without hiring a lawyer."
    ),
    LegalEntry(
        term = "Sua Sponte",
        category = "Judicial Authority",
        definition = "A Latin phrase meaning 'of one's own accord.' Describes an action taken by a court on its own initiative, without a motion or request by any party.",
        plainEnglish = "When the judge steps in and does something on their own — without either side asking them to."
    ),
    LegalEntry(
        term = "Summary Judgment",
        category = "Civil Procedure",
        definition = "A judgment entered by the court for one party against another without a full trial, where the moving party demonstrates that there is no genuine dispute of material fact and they are entitled to judgment as a matter of law.",
        plainEnglish = "A ruling where the judge decides the case without a trial because the facts aren't in dispute and the law clearly favors one side."
    ),
    LegalEntry(
        term = "Estoppel",
        category = "Equity / Contract Law",
        definition = "A legal principle that prevents a party from asserting a claim or position that is contradicted by their own prior conduct or statements, when another party has reasonably relied on that prior conduct to their detriment.",
        plainEnglish = "A rule that stops someone from arguing one thing in court when they previously said or did the opposite, especially if you relied on what they said."
    ),
    LegalEntry(
        term = "Voir Dire",
        category = "Jury Selection",
        definition = "A preliminary examination of prospective jurors by the judge and/or attorneys to determine competency, impartiality, and suitability for jury service. Also used to examine the qualifications of an expert witness.",
        plainEnglish = "The questioning process where lawyers and the judge interview potential jurors to decide if they can be fair. Also used to challenge an expert witness before they testify."
    ),
    LegalEntry(
        term = "Affidavit",
        category = "Evidence / Procedure",
        definition = "A written statement of facts made voluntarily and confirmed by the oath or affirmation of the party making it, taken before an officer having authority to administer such an oath.",
        plainEnglish = "A written, sworn statement — you're signing it under oath, meaning you can face perjury charges if you lie."
    )
)

// ---------------------------------------------------------------------------
// Case Caption Formatter
// ---------------------------------------------------------------------------

/**
 * Formats a formal court case caption from the provided names and jurisdiction.
 *
 * @param judgeName       The presiding judge's last name (or full name).
 * @param plaintiffName   The plaintiff's name as it appears on the docket.
 * @param defendantName   The defendant's name as it appears on the docket.
 * @param jurisdiction    The jurisdiction string, e.g. "CALIFORNIA" (defaults to "[JURISDICTION]").
 * @return A multi-line string containing the formatted caption block.
 */
fun formatCaseCaption(
    judgeName: String,
    plaintiffName: String,
    defendantName: String,
    jurisdiction: String = "[JURISDICTION]"
): String {
    val judge     = judgeName.trim().ifBlank { "[JUDGE NAME]" }
    val plaintiff = plaintiffName.trim().ifBlank { "[PLAINTIFF]" }
    val defendant = defendantName.trim().ifBlank { "[DEFENDANT]" }
    val jur       = jurisdiction.trim().ifBlank { "[JURISDICTION]" }.uppercase()

    return buildString {
        appendLine("IN THE COURT OF THE STATE OF $jur")
        appendLine()
        appendLine("$plaintiff, Plaintiff,")
        appendLine()
        appendLine("        v.")
        appendLine()
        appendLine("$defendant, Defendant.")
        appendLine()
        appendLine("PRESIDING: Hon. $judge")
    }
}

// ---------------------------------------------------------------------------
// Folk-to-Formal Etiquette Processor
// ---------------------------------------------------------------------------

private val etiquetteMappings: List<Pair<Regex, String>> = listOf(

    // ── Credibility / Lying ──────────────────────────────────────────────────
    Pair(
        Regex("(?:the )?(?:officer|cop|detective|police|agent|officer) (?:lied|is lying|was lying|lied to (?:me|us|the court))", RegexOption.IGNORE_CASE),
        "the law enforcement officer's testimony is materially inconsistent with documented physical evidence, constituting false swearing before this Court"
    ),
    Pair(
        Regex("he(?:'s| is) lying|she(?:'s| is) lying|they(?:'re| are) lying", RegexOption.IGNORE_CASE),
        "the opposing party has provided an account that is materially inconsistent with the verified record before this Court"
    ),
    Pair(
        Regex("(?:the )?witness is (?:lying|a liar|not telling the truth|making (?:it|things) up)", RegexOption.IGNORE_CASE),
        "the testimony of the complaining witness contains material inconsistencies, is devoid of independent corroboration, and fails to satisfy the threshold of reliability required under the applicable rules of evidence"
    ),
    Pair(
        Regex("lied about|told lies?|he is a liar|she is a liar|they are liars?", RegexOption.IGNORE_CASE),
        "provided testimony containing material misrepresentations directly contradicted by the established record"
    ),

    // ── Fourth Amendment — Searches, Seizures & Arrests ──────────────────────
    Pair(
        Regex("(?:they|he|she|police|cops|officers?) (?:broke into|searched|entered|came into|went through) (?:my|the) (?:home|house|apartment|car|vehicle|property) (?:without (?:a )?warrant|illegally|unlawfully)", RegexOption.IGNORE_CASE),
        "law enforcement officers conducted a warrantless intrusion into Defendant's constitutionally protected premises in direct violation of the Fourth Amendment to the United States Constitution, rendering all evidence seized therein subject to suppression under the exclusionary rule"
    ),
    Pair(
        Regex("(?:the )?(?:arrest|stop|detention) was (?:wrong|illegal|unlawful|without cause|not right|unjust|bad|unconstitutional)", RegexOption.IGNORE_CASE),
        "Defendant's arrest, stop, and/or detention was effectuated without probable cause in direct contravention of the Fourth Amendment's guarantee against unreasonable seizures of the person, warranting suppression of all derivative evidence"
    ),
    Pair(
        Regex("(?:they|police|officers?) (?:planted|put|placed|hid|fabricated|manufactured) (?:the )?(?:drugs?|evidence|gun|weapon|contraband)", RegexOption.IGNORE_CASE),
        "the physical evidence introduced against Defendant was unlawfully manufactured or placed by agents of the State in violation of established chain-of-custody protocols and due process principles, rendering it wholly inadmissible"
    ),
    Pair(
        Regex("(?:the )?search was (?:illegal|unlawful|wrong|unauthorized|bad|without a warrant)", RegexOption.IGNORE_CASE),
        "the warrantless search and seizure was conducted in the absence of probable cause, valid consent, exigent circumstances, or any other recognized exception to the Fourth Amendment's warrant requirement"
    ),

    // ── Miranda / Fifth Amendment ─────────────────────────────────────────────
    Pair(
        Regex("(?:they|police|officers?) didn'?t (?:read|give me|tell me|say) (?:my |the )?miranda(?: rights?)?|didn'?t read me my rights", RegexOption.IGNORE_CASE),
        "law enforcement officers failed to administer the constitutionally mandated Miranda warnings prior to custodial interrogation; accordingly, all statements obtained therefrom are inadmissible under Miranda v. Arizona, 384 U.S. 436 (1966) and its progeny"
    ),
    Pair(
        Regex("(?:my )?confession (?:was|is) (?:fake|false|forced|coerced|not real|not true|made up)", RegexOption.IGNORE_CASE),
        "the purported confession was obtained through coercive interrogation tactics that vitiated Defendant's free will, rendering the statement involuntary and inadmissible under the Due Process Clause of the Fourteenth Amendment and Colorado v. Connelly, 479 U.S. 157 (1986)"
    ),
    Pair(
        Regex("(?:they|i was) (?:forced|made|coerced) (?:to |me to )?(?:sign|agree|confess|admit|say)", RegexOption.IGNORE_CASE),
        "any statement, signature, or admission attributed to Defendant was obtained through unlawful coercion, physical duress, and/or psychological pressure, and is therefore void and constitutionally inadmissible"
    ),

    // ── Sixth Amendment / Right to Counsel ───────────────────────────────────
    Pair(
        Regex("didn'?t (?:let me|give me|allow me to) (?:have|see|talk to|speak to|call) (?:a |my )?(?:lawyer|attorney|counsel)", RegexOption.IGNORE_CASE),
        "Defendant was unlawfully denied access to counsel at a critical stage of the proceedings, in direct violation of the Sixth Amendment's guarantee of the right to the assistance of counsel as construed in Massiah v. United States, 377 U.S. 201 (1964)"
    ),
    Pair(
        Regex("(?:my )?(?:lawyer|attorney|counsel) is (?:bad|terrible|not helping|useless|incompetent|not doing (?:his|her|their) job)", RegexOption.IGNORE_CASE),
        "trial counsel rendered constitutionally deficient assistance falling below the objective standard of reasonable professional judgment, causing Defendant prejudice within the meaning of Strickland v. Washington, 466 U.S. 668 (1984)"
    ),

    // ── Brady / Prosecution Misconduct ───────────────────────────────────────
    Pair(
        Regex("(?:they|prosecution|prosecutor) (?:hid|suppressed|buried|withheld|concealed) (?:the |my )?(?:evidence|proof|records?)", RegexOption.IGNORE_CASE),
        "the prosecution willfully suppressed material evidence favorable to the defense, constituting a Brady violation of constitutional magnitude warranting dismissal or, in the alternative, appropriate curative sanctions"
    ),
    Pair(
        Regex("(?:they|prosecution|prosecutor) didn'?t (?:tell|show|give|share|disclose|turn over|provide) (?:me|us|the defense) (?:the |about the )?evidence", RegexOption.IGNORE_CASE),
        "the prosecution has failed to discharge its mandatory disclosure obligations under Brady v. Maryland, 373 U.S. 83 (1963), and Giglio v. United States, 405 U.S. 150 (1972), thereby depriving Defendant of material exculpatory and impeachment evidence in violation of due process"
    ),

    // ── Charging Instrument / Case Dismissal ─────────────────────────────────
    Pair(
        Regex("(?:the )?charges are (?:made up|false|fake|not true|bogus|wrong|lies|baseless)", RegexOption.IGNORE_CASE),
        "the charging instrument lacks a sufficient factual predicate and fails to allege all elements necessary to sustain prosecution, warranting dismissal as a matter of law"
    ),
    Pair(
        Regex("(?:this case|the case|it) should be (?:thrown out|dismissed|dropped|gone|over)", RegexOption.IGNORE_CASE),
        "the instant matter is subject to dismissal with prejudice on the grounds that the prosecution has failed to state a legally cognizable claim upon which relief may be granted"
    ),
    Pair(
        Regex("(?:i was |they )?set (?:me )?up|(?:it was|this is) (?:a )?setup", RegexOption.IGNORE_CASE),
        "the events forming the basis of the instant charges were the direct product of improper government inducement and outrageous law enforcement conduct constituting entrapment as a matter of law under Jacobson v. United States, 503 U.S. 540 (1992)"
    ),
    Pair(
        Regex("(?:they'?re?|they are|prosecution is) out to get me|targeting me|going after me|selective(?:ly)? prosecut", RegexOption.IGNORE_CASE),
        "the prosecution's course of conduct reflects a pattern of selective, vindictive, and retaliatory prosecution undertaken in bad faith in direct violation of Defendant's equal protection and due process rights under the Fourteenth Amendment"
    ),

    // ── Judicial Conduct ─────────────────────────────────────────────────────
    Pair(
        Regex("(?:the )?judge is (?:biased|unfair|against me|out to get me|partial|not fair|corrupt)", RegexOption.IGNORE_CASE),
        "the presiding judicial officer has engaged in conduct giving rise to a reasonable question of impartiality, warranting recusal pursuant to the applicable canons of judicial conduct and 28 U.S.C. § 455(a)"
    ),
    Pair(
        Regex("i want a fair trial|give me a fair trial|just want (?:a |to be treated )?fair(?:ly)?", RegexOption.IGNORE_CASE),
        "Defendant respectfully invokes his constitutional right to a fair and impartial trial by jury as guaranteed by the Sixth and Fourteenth Amendments to the United States Constitution"
    ),
    Pair(
        Regex("didn'?t follow (?:the )?rules?|broke the rules?|violated procedure|didn'?t follow (?:proper )?procedure", RegexOption.IGNORE_CASE),
        "the opposing party has failed to comply with applicable procedural requirements, statutory mandates, and rules of court, thereby causing substantial prejudice to Defendant's substantive rights"
    ),

    // ── Innocence & Denial ────────────────────────────────────────────────────
    Pair(
        Regex("i didn'?t do it|i didn'?t do (?:anything|nothing)|it wasn'?t me|i(?:'m| am) innocent", RegexOption.IGNORE_CASE),
        "Defendant categorically denies each and every allegation set forth in the charging instrument and maintains his absolute innocence of the charged offense"
    ),
    Pair(
        Regex("(?:he|she|they|it) did it|it was (?:him|her|them|someone else)", RegexOption.IGNORE_CASE),
        "the competent and credible evidence before this Court establishes that responsibility for the charged conduct lies with a party other than Defendant"
    ),

    // ── Assault / Theft ───────────────────────────────────────────────────────
    Pair(
        Regex("(?:he|she|they) (?:hit|beat|assaulted|attacked|struck|punched|kicked) me", RegexOption.IGNORE_CASE),
        "the opposing party subjected Defendant to unlawful physical battery, constituting an intentional and unprovoked assault in violation of applicable criminal and civil statutes"
    ),
    Pair(
        Regex("(?:he|she|they) (?:stole|took|robbed) (?:from )?me|stole my|took my", RegexOption.IGNORE_CASE),
        "the opposing party unlawfully converted Defendant's personal property to their own use without authorization, consent, or any lawful justification"
    ),
    Pair(
        Regex("(?:he|she|they) (?:threatened|intimidated) me", RegexOption.IGNORE_CASE),
        "Defendant was subjected to explicit threats and coercive intimidation that vitiated the voluntariness of any statement or conduct attributed to him"
    ),

    // ── Miscellaneous ─────────────────────────────────────────────────────────
    Pair(
        Regex("i have proof|i have evidence|i can prove (?:it|this)", RegexOption.IGNORE_CASE),
        "Defendant is in possession of material exculpatory evidence directly probative of the central issues presented to this Court"
    ),
    Pair(
        Regex("i need more time|give me more time|need a continuance", RegexOption.IGNORE_CASE),
        "Defendant respectfully moves this Court for a reasonable continuance sufficient to afford constitutionally adequate preparation of the defense"
    ),
    Pair(
        Regex("it'?s? not fair|that'?s? not fair|this is not fair|it is not fair", RegexOption.IGNORE_CASE),
        "the conduct complained of constitutes a material deprivation of Defendant's right to due process of law under the Fifth and Fourteenth Amendments to the United States Constitution"
    ),
    Pair(
        Regex("i don'?t remember|i can'?t remember|i don'?t recall", RegexOption.IGNORE_CASE),
        "Defendant has no independent present recollection of the events, transactions, or occurrences forming the subject matter of this inquiry"
    ),
    Pair(
        Regex("(?:he|she|they|the prosecutor|opposing counsel|the other side) (?:is|are) wrong", RegexOption.IGNORE_CASE),
        "the position advanced by opposing counsel is unsupported by controlling legal authority and is directly contradicted by the established facts of record"
    ),
    Pair(
        Regex("i want to stop this(?: now)?|stop this now|make this stop|make it stop", RegexOption.IGNORE_CASE),
        "Defendant respectfully moves this Court for an immediate stay of all proceedings pending full resolution of the constitutional and procedural issues raised herein"
    ),
    Pair(
        Regex("this is (?:all )?wrong|everything is wrong|that'?s? wrong", RegexOption.IGNORE_CASE),
        "the record as presented is materially incorrect, fundamentally prejudicial, and directly inconsistent with the applicable law and established facts"
    ),
    Pair(
        Regex("(?:he|she|they)'?re? guilty|(?:he|she|they) (?:is|are) guilty", RegexOption.IGNORE_CASE),
        "the totality of the evidence before this Court establishes the opposing party's culpability beyond any reasonable doubt"
    )
)

/**
 * Converts casual, emotional language into formal courtroom etiquette.
 *
 * The function applies known phrase substitutions and then wraps the result
 * with the standard formal phrasing used in courtroom submissions.
 *
 * @param rawInput   The unprocessed, colloquial text entered by the user.
 * @return A formally wrapped string suitable for courtroom use.
 */
/**
 * Applies all legal-language substitutions to [rawInput] without adding any
 * courtroom wrapper. Used internally by [processEtiquette] and called directly
 * from the motion generator to legalize plain-language goal/fact text.
 */
fun legalizeText(rawInput: String): String {
    var processed = rawInput.trim()
    for ((pattern, replacement) in etiquetteMappings) {
        processed = pattern.replace(processed, replacement)
    }
    if (processed.isNotEmpty()) {
        processed = processed.replaceFirstChar { it.uppercaseChar() }
        if (!processed.endsWith('.') && !processed.endsWith('?') && !processed.endsWith('!')) {
            processed += "."
        }
    }
    return processed
}

fun processEtiquette(rawInput: String): String {
    val processed = legalizeText(rawInput)
    return buildString {
        appendLine("May it please the Court,")
        appendLine()
        appendLine(processed)
        appendLine()
        append("Respectfully submitted,")
    }
}

// ---------------------------------------------------------------------------
// Dictionary Screen composable
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtroomDictionaryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }

    // Caption formatter state
    var captionJudge by remember { mutableStateOf("") }
    var captionPlaintiff by remember { mutableStateOf("") }
    var captionDefendant by remember { mutableStateOf("") }
    var captionJurisdiction by remember { mutableStateOf("") }
    var captionOutput by remember { mutableStateOf("") }

    // Etiquette processor state
    var etiquetteInput by remember { mutableStateOf("") }
    var etiquetteOutput by remember { mutableStateOf("") }

    val filteredTerms = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            courtroomTerms
        } else {
            val q = searchQuery.trim().lowercase()
            courtroomTerms.filter { entry ->
                entry.term.lowercase().contains(q) ||
                    entry.category.lowercase().contains(q) ||
                    entry.definition.lowercase().contains(q) ||
                    entry.plainEnglish.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Courtroom Dictionary", fontWeight = FontWeight.Bold)
                        Text(
                            "Legal terms explained in plain English",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search terms") },
                        placeholder = { Text("hearsay, estoppel, voir dire…") },
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true
                    )
                }

                // Term count indicator
                item {
                    Text(
                        text = if (searchQuery.isBlank())
                            "${courtroomTerms.size} terms"
                        else
                            "${filteredTerms.size} of ${courtroomTerms.size} terms",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Term cards
                items(filteredTerms, key = { it.term }) { entry ->
                    LegalTermCard(entry = entry, onCopy = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    })
                }

                if (filteredTerms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No terms match \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Divider before tools section
                item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

                // ── Case Caption Formatter ──────────────────────────────────────
                item {
                    DictionaryToolCard(title = "Case Caption Formatter") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Enter names to generate a formal caption block.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = captionJudge,
                                onValueChange = { captionJudge = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Judge's name") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = captionPlaintiff,
                                onValueChange = { captionPlaintiff = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Plaintiff name") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = captionDefendant,
                                onValueChange = { captionDefendant = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Defendant name") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = captionJurisdiction,
                                onValueChange = { captionJurisdiction = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Jurisdiction (e.g. CALIFORNIA)") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        captionOutput = formatCaseCaption(
                                            judgeName = captionJudge,
                                            plaintiffName = captionPlaintiff,
                                            defendantName = captionDefendant,
                                            jurisdiction = captionJurisdiction
                                        )
                                    }
                                ) {
                                    Text("Format Caption")
                                }
                                if (captionOutput.isNotBlank()) {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(captionOutput))
                                            Toast.makeText(context, "Caption copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy")
                                    }
                                }
                            }
                            if (captionOutput.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = captionOutput,
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Folk-to-Formal Etiquette Processor ─────────────────────────
                item {
                    DictionaryToolCard(title = "Etiquette Processor") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Type casual language and receive formal courtroom phrasing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = etiquetteInput,
                                onValueChange = { etiquetteInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("What do you want to say?") },
                                placeholder = { Text("He's lying, it's not fair…") },
                                minLines = 3,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        etiquetteOutput = processEtiquette(etiquetteInput)
                                    },
                                    enabled = etiquetteInput.isNotBlank()
                                ) {
                                    Text("Make It Formal")
                                }
                                if (etiquetteOutput.isNotBlank()) {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(etiquetteOutput))
                                            Toast.makeText(context, "Formal text copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy")
                                    }
                                }
                            }
                            if (etiquetteOutput.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = etiquetteOutput,
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Supporting composables
// ---------------------------------------------------------------------------

@Composable
private fun LegalTermCard(entry: LegalEntry, onCopy: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Term header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.term,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = entry.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                IconButton(
                    onClick = {
                        onCopy("${entry.term}\n\n${entry.definition}\n\nPlain English: ${entry.plainEnglish}")
                    }
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy ${entry.term}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Formal definition
            Text(
                text = "Definition",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = entry.definition,
                style = MaterialTheme.typography.bodyMedium
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Plain English
            Text(
                text = "Plain English",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = entry.plainEnglish,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DictionaryToolCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}
