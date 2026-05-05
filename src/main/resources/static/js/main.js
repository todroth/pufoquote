(function () {
  const nextBtn = document.getElementById('next-btn');
  const catBtns = document.querySelectorAll('.cat-btn');

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
      loadQuote(nextBtn.dataset.category || 'RANDOM');
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

  function renderQuote(q) {
    const card  = document.getElementById('quote-card');
    const empty = document.getElementById('quote-empty');

    if (empty) empty.style.display = 'none';
    if (card)  card.style.display  = '';

    document.getElementById('quote-text').textContent = q.text;

    const ep = document.getElementById('quote-episode');
    ep.textContent = q.episodeName || '';
    ep.href = q.episodeUrl || '#';

    document.getElementById('quote-timestamp').textContent = q.timestamp || '';
  }

  function showEmpty() {
    const card  = document.getElementById('quote-card');
    const empty = document.getElementById('quote-empty');
    if (card)  card.style.display  = 'none';
    if (empty) empty.style.display = '';
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

  // keep JS navigation in sync with browser back/forward
  window.addEventListener('popstate', function (e) {
    const category = (e.state && e.state.category) || 'RANDOM';
    setActiveCategory(category);
    loadQuote(category);
  });
}());
