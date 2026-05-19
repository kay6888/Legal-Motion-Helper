(function initMotionGenerator() {
  const form = document.getElementById('motion-form');
  const output = document.getElementById('draft-output');
  const errors = document.getElementById('validation-errors');
  const status = document.getElementById('status');
  const generateBtn = document.getElementById('generate-btn');
  const clearBtn = document.getElementById('clear-btn');
  const copyBtn = document.getElementById('copy-btn');
  const downloadBtn = document.getElementById('download-btn');

  const storageKey = 'legal-motion-helper.form.v1';

  const templates = {
    'Motion to Continue': 'Good cause exists for a continuance because proceeding on the current schedule would materially prejudice the moving party.',
    'Motion to Dismiss': 'The pleadings and applicable law do not support continuation of the challenged claims.',
    'Motion to Compel': 'The opposing party has not provided discovery required by the governing rules despite reasonable requests.',
    'Motion for Summary Judgment': 'There is no genuine dispute of material fact, and the moving party is entitled to judgment as a matter of law.',
    'Motion for Protective Order': 'A protective order is needed to prevent undue burden, harassment, or disclosure of sensitive information.',
    'Motion in Limine': 'Pretrial exclusion is required to avoid unfair prejudice, jury confusion, or introduction of inadmissible evidence.',
    Other: 'The requested order is legally and factually justified under the circumstances of this case.'
  };

  loadDraftState();

  form.addEventListener('input', debounce(storeDraftState, 200));
  form.addEventListener('submit', handleGenerate);
  form.addEventListener('keydown', (event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      form.requestSubmit();
    }
  });

  clearBtn.addEventListener('click', handleClear);
  copyBtn.addEventListener('click', handleCopy);
  downloadBtn.addEventListener('click', handleDownload);

  async function handleGenerate(event) {
    event.preventDefault();
    hide(errors);
    showInfo('Generating draft...');

    const formData = getFormData();
    const validation = validateForm(formData);

    if (validation.length) {
      showErrors(validation);
      hide(status);
      return;
    }

    setBusy(true);

    try {
      await waitForPaint();
      const draft = buildDraft(formData);
      output.textContent = draft;
      copyBtn.disabled = false;
      downloadBtn.disabled = false;
      showInfo('Draft generated. Review carefully and customize for your jurisdiction.');
    } catch (err) {
      showErrors(['Generation failed unexpectedly. Please try again.']);
      console.error(err);
      hide(status);
    } finally {
      setBusy(false);
    }
  }

  function getFormData() {
    const fd = new FormData(form);
    return {
      courtName: clean(fd.get('courtName')),
      caseNumber: clean(fd.get('caseNumber')),
      partyName: clean(fd.get('partyName')),
      opposingParty: clean(fd.get('opposingParty')),
      motionType: clean(fd.get('motionType')),
      hearingDate: clean(fd.get('hearingDate')),
      factualBasis: clean(fd.get('factualBasis')),
      legalBasis: clean(fd.get('legalBasis')),
      reliefRequested: clean(fd.get('reliefRequested')),
      notes: clean(fd.get('notes'))
    };
  }

  function validateForm(data) {
    const validationErrors = [];

    [
      ['courtName', 'Court Name'],
      ['caseNumber', 'Case Number'],
      ['partyName', 'Moving Party'],
      ['motionType', 'Motion Type'],
      ['factualBasis', 'Factual Basis'],
      ['reliefRequested', 'Requested Relief']
    ].forEach(([key, label]) => {
      if (!data[key]) validationErrors.push(`${label} is required.`);
    });

    if (data.factualBasis && data.factualBasis.length < 40) {
      validationErrors.push('Factual Basis should include enough detail (at least 40 characters).');
    }

    if (data.hearingDate && Number.isNaN(Date.parse(data.hearingDate))) {
      validationErrors.push('Hearing Date must be a valid date.');
    }

    return validationErrors;
  }

  function buildDraft(data) {
    const now = new Date();
    const today = now.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
    const supportSentence = templates[data.motionType] || templates.Other;
    const legalBasis = data.legalBasis
      ? `This motion is further supported by the following authority: ${data.legalBasis}`
      : 'This motion is based on applicable procedural rules and the Court\'s inherent authority to manage proceedings fairly and efficiently.';

    const hearingLine = data.hearingDate
      ? `A hearing is requested for ${formatDate(data.hearingDate)} or the earliest date available to the Court.`
      : 'A hearing is requested at the Court\'s earliest availability.';

    const caption = [
      data.courtName.toUpperCase(),
      '',
      `${data.partyName},`,
      '  Moving Party,',
      data.opposingParty ? `v.\n${data.opposingParty},\n  Opposing Party,` : '',
      '',
      `Case No.: ${data.caseNumber}`,
      `RE: ${data.motionType}`
    ].filter(Boolean).join('\n');

    return [
      caption,
      '',
      `${data.motionType.toUpperCase()}`,
      '',
      `COMES NOW ${data.partyName}, and respectfully moves this Court for relief as follows:`,
      '',
      'I. INTRODUCTION',
      `${supportSentence}`,
      '',
      'II. FACTUAL BACKGROUND',
      `${data.factualBasis}`,
      '',
      'III. LEGAL ARGUMENT',
      `${legalBasis}`,
      '',
      'IV. REQUESTED RELIEF',
      `${data.reliefRequested}`,
      `${hearingLine}`,
      data.notes ? `Additional context: ${data.notes}` : '',
      '',
      'V. CONCLUSION',
      `For these reasons, ${data.partyName} respectfully requests that the Court grant this ${data.motionType.toLowerCase()} and award any other just and proper relief.`,
      '',
      `Dated: ${today}`,
      '',
      'Respectfully submitted,',
      '',
      `${data.partyName}`
    ].filter(Boolean).join('\n');
  }

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(output.textContent || '');
      showInfo('Draft copied to clipboard.');
    } catch {
      showErrors(['Unable to copy draft. Your browser may block clipboard access.']);
    }
  }

  function handleDownload() {
    const data = output.textContent || '';
    if (!data || data === 'Your generated motion will appear here.') {
      return;
    }

    const blob = new Blob([data], { type: 'text/plain;charset=utf-8' });
    const link = document.createElement('a');
    const safeType = (getFormData().motionType || 'motion').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
    link.href = URL.createObjectURL(blob);
    link.download = `${safeType || 'motion'}-${new Date().toISOString().slice(0, 10)}.txt`;
    link.click();
    URL.revokeObjectURL(link.href);
    showInfo('Draft downloaded as text file.');
  }

  function handleClear() {
    form.reset();
    localStorage.removeItem(storageKey);
    output.textContent = 'Your generated motion will appear here.';
    hide(errors);
    hide(status);
    copyBtn.disabled = true;
    downloadBtn.disabled = true;
  }

  function setBusy(isBusy) {
    generateBtn.disabled = isBusy;
    generateBtn.textContent = isBusy ? 'Generating...' : 'Generate Motion';
  }

  function showErrors(items) {
    errors.innerHTML = `<ul>${items.map((item) => `<li>${item}</li>`).join('')}</ul>`;
    show(errors);
  }

  function showInfo(message) {
    status.textContent = message;
    show(status);
  }

  function show(el) {
    el.classList.remove('hidden');
  }

  function hide(el) {
    el.classList.add('hidden');
  }

  function loadDraftState() {
    try {
      const json = localStorage.getItem(storageKey);
      if (!json) return;
      const saved = JSON.parse(json);
      Object.entries(saved).forEach(([name, value]) => {
        if (form.elements[name] && typeof value === 'string') {
          form.elements[name].value = value;
        }
      });
    } catch (err) {
      console.warn('Unable to load saved draft state.', err);
    }
  }

  function storeDraftState() {
    const data = getFormData();
    localStorage.setItem(storageKey, JSON.stringify(data));
  }

  function clean(value) {
    return String(value || '').replace(/\s+/g, ' ').trim();
  }

  function debounce(fn, delayMs) {
    let timer;
    return (...args) => {
      clearTimeout(timer);
      timer = setTimeout(() => fn(...args), delayMs);
    };
  }

  function formatDate(value) {
    return new Date(value + 'T00:00:00').toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  function waitForPaint() {
    return new Promise((resolve) => requestAnimationFrame(() => setTimeout(resolve, 0)));
  }
})();
