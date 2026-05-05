(function () {
  const nextBtn  = document.getElementById('next-btn');
  const homeLink = document.getElementById('home-link');
  const catBtns  = document.querySelectorAll('.cat-btn');
  const shareBtn = document.getElementById('share-btn');

  if (homeLink) {
    homeLink.addEventListener('click', function (e) {
      e.preventDefault();
      setActiveCategory('RANDOM');
      loadQuote('RANDOM');
      history.pushState({category: 'RANDOM'}, '', '/');
    });
  }

  catBtns.forEach(function (btn) {
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      const category = btn.dataset.category;
      setActiveCategory(category);
      loadQuote(category);
      history.pushState({category: category}, '', '/?category=' + encodeURIComponent(category));
    });
  });

  if (nextBtn) {
    nextBtn.addEventListener('click', function (e) {
      e.preventDefault();
      const category = nextBtn.dataset.category || 'RANDOM';
      history.pushState({category: category}, '', '/?category=' + encodeURIComponent(category));
      loadQuote(category);
    });
  }

  async function loadQuote(category) {
    try {
      const res = await fetch('/api/quote?category=' + encodeURIComponent(category));
      if (res.status === 204) { showEmpty(); return; }
      if (!res.ok) throw new Error();
      renderQuote(await res.json());
    } catch (_) {
      window.location.href = '/?category=' + encodeURIComponent(category);
    }
  }

  async function loadQuoteById(id) {
    try {
      const res = await fetch('/api/quote/' + encodeURIComponent(id));
      if (!res.ok) throw new Error();
      renderQuote(await res.json());
    } catch (_) {
      window.location.href = '/quote/' + encodeURIComponent(id);
    }
  }

  function renderQuote(q) {
    const card  = document.getElementById('quote-card');
    const empty = document.getElementById('quote-empty');

    if (empty) empty.style.display = 'none';
    if (card)  {
      card.style.display  = '';
      card.dataset.quoteId = q.id || '';
    }

    document.getElementById('quote-text').textContent = q.text;

    const ep = document.getElementById('quote-episode');
    ep.textContent = q.episodeName || '';
    ep.href = q.episodeUrl || '#';

    document.getElementById('quote-timestamp').textContent = q.timestamp ? '~' + q.timestamp : '';

    resetContext();
  }

  function showEmpty() {
    const card  = document.getElementById('quote-card');
    const empty = document.getElementById('quote-empty');
    if (card)  card.style.display  = 'none';
    if (empty) empty.style.display = '';
  }

  function resetContext() {
    const before = document.getElementById('context-before');
    const after  = document.getElementById('context-after');
    const btn    = document.getElementById('context-btn');
    if (before) { before.innerHTML = ''; before.style.display = 'none'; }
    if (after)  { after.innerHTML  = ''; after.style.display  = 'none'; }
    if (btn)    { btn.textContent = 'Kontext'; btn.classList.remove('active'); }
  }

  const contextBtn  = document.getElementById('context-btn');
  const quoteText   = document.getElementById('quote-text');

  async function toggleContext() {
    const card = document.getElementById('quote-card');
    const quoteId = card && card.dataset.quoteId;
    if (!quoteId) return;

    const before = document.getElementById('context-before');
    const after  = document.getElementById('context-after');

    if (contextBtn && contextBtn.classList.contains('active')) {
      resetContext();
      return;
    }

    try {
      const res = await fetch('/api/quote/' + encodeURIComponent(quoteId) + '/context');
      if (!res.ok) return;
      const ctx = await res.json();

      before.innerHTML = '';
      if (ctx.before && ctx.before.length) {
        const p = document.createElement('p');
        p.className = 'context-sentence';
        p.textContent = ctx.before.join(' ');
        before.appendChild(p);
        before.style.display = '';
      } else {
        before.style.display = 'none';
      }

      after.innerHTML = '';
      if (ctx.after && ctx.after.length) {
        const p = document.createElement('p');
        p.className = 'context-sentence';
        p.textContent = ctx.after.join(' ');
        after.appendChild(p);
        after.style.display = '';
      } else {
        after.style.display = 'none';
      }

      if (contextBtn) {
        contextBtn.textContent = 'Kontext verstecken';
        contextBtn.classList.add('active');
      }
    } catch (_) {
      // silently ignore context load failures
    }
  }

  if (contextBtn) {
    contextBtn.addEventListener('click', toggleContext);
  }

  if (quoteText) {
    quoteText.addEventListener('click', toggleContext);
  }

  function setActiveCategory(category) {
    catBtns.forEach(function (btn) {
      btn.classList.toggle('active', btn.dataset.category === category);
    });
    if (nextBtn) {
      nextBtn.dataset.category = category;
      nextBtn.href = '/?category=' + encodeURIComponent(category);
    }
  }

  if (shareBtn) {
    shareBtn.addEventListener('click', function () {
      const card = document.getElementById('quote-card');
      const quoteId = card && card.dataset.quoteId;
      if (!quoteId) return;

      const url = window.location.origin + '/quote/' + quoteId;
      history.pushState({quoteId: quoteId}, '', '/quote/' + quoteId);

      if (navigator.clipboard) {
        navigator.clipboard.writeText(url).then(function () {
          shareBtn.classList.add('copied');
          setTimeout(function () { shareBtn.classList.remove('copied'); }, 1500);
        });
      }
    });
  }

  // keep JS navigation in sync with browser back/forward
  window.addEventListener('popstate', function (e) {
    if (e.state && e.state.quoteId) {
      loadQuoteById(e.state.quoteId);
      return;
    }
    const category = (e.state && e.state.category) || 'RANDOM';
    setActiveCategory(category);
    loadQuote(category);
  });
}());
