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
      const category = btn.dataset.category;
      if (!category) return;
      e.preventDefault();
      setActiveCategory(category);
      loadQuote(category);
    });
  });

  if (nextBtn) {
    nextBtn.addEventListener('click', function (e) {
      e.preventDefault();
      const category = nextBtn.dataset.category || 'RANDOM';
      loadQuote(category);
    });
  }

  async function loadQuote(category) {
    try {
      const res = await fetch('/api/quote?category=' + encodeURIComponent(category));
      if (res.status === 204) { showEmpty(); return; }
      if (!res.ok) throw new Error();
      const q = await res.json();
      renderQuote(q);
      if (q.id) history.replaceState({quoteId: q.id, category: category}, '', '/quote/' + q.id);
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

    document.getElementById('quote-category').textContent = q.category || '';
    document.getElementById('quote-date').textContent = q.episodeDate || '';
    document.getElementById('quote-timestamp').textContent = q.timestamp ? '~' + q.timestamp : '';

    const likeBtn = document.getElementById('like-btn');
    if (likeBtn) {
      likeBtn.dataset.alreadyLiked = q.alreadyVoted ? 'true' : 'false';
      likeBtn.classList.toggle('liked', !!q.alreadyVoted);
      const likeCount = likeBtn.querySelector('.like-count');
      if (likeCount) likeCount.textContent = q.voteCount || 0;
    }

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

  const likeBtn = document.getElementById('like-btn');
  if (likeBtn) {
    likeBtn.addEventListener('click', async function () {
      const card = document.getElementById('quote-card');
      const quoteId = card && card.dataset.quoteId;
      if (!quoteId) return;
      try {
        const res = await fetch('/api/quote/' + encodeURIComponent(quoteId) + '/vote', {
          method: 'POST',
        });
        if (!res.ok) return;
        const data = await res.json();
        likeBtn.dataset.alreadyLiked = data.alreadyVoted ? 'true' : 'false';
        likeBtn.classList.toggle('liked', data.alreadyVoted);
        const likeCount = likeBtn.querySelector('.like-count');
        if (likeCount) likeCount.textContent = data.voteCount;
      } catch (_) {}
    });
  }

  // ── Best-of page: load more ──
  const loadMoreBtn = document.getElementById('load-more-btn');
  if (loadMoreBtn) {
    let offset = parseInt(loadMoreBtn.dataset.offset || '20', 10);
    loadMoreBtn.addEventListener('click', async function () {
      loadMoreBtn.disabled = true;
      loadMoreBtn.textContent = '…';
      try {
        const res = await fetch('/api/best?offset=' + offset + '&limit=20');
        if (!res.ok) throw new Error();
        const items = await res.json();
        const list = document.getElementById('best-list');
        items.forEach(function (item) {
          const li = document.createElement('li');
          li.className = 'best-item';
          li.innerHTML =
            '<a href="/quote/' + escHtml(item.quote.id) + '" class="best-item-link">' +
            '<span class="best-rank-badge">' + escHtml(String(item.voteCount)) + '</span>' +
            '<div class="best-item-body">' +
            '<p class="best-quote-text">' + escHtml(item.quote.text) + '</p>' +
            '<span class="best-meta">' + escHtml(item.quote.episodeName) + ' · ' + escHtml(item.quote.episodeDate) + '</span>' +
            '</div></a>';
          list.appendChild(li);
        });
        offset += items.length;
        if (items.length < 20) {
          loadMoreBtn.style.display = 'none';
        } else {
          loadMoreBtn.disabled = false;
          loadMoreBtn.textContent = 'Mehr laden';
        }
      } catch (_) {
        loadMoreBtn.disabled = false;
        loadMoreBtn.textContent = 'Mehr laden';
      }
    });
  }

  function escHtml(str) {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // keep JS navigation in sync with browser back/forward
  window.addEventListener('popstate', function (e) {
    if (e.state && e.state.quoteId) {
      if (e.state.category) setActiveCategory(e.state.category);
      loadQuoteById(e.state.quoteId);
      return;
    }
    const category = (e.state && e.state.category) || 'RANDOM';
    setActiveCategory(category);
    loadQuote(category);
  });
}());
