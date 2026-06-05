(function () {
    const body = document.body;
    const sidebar = document.querySelector('.sidebar');

    if (!body || !sidebar || document.querySelector('.mobile-topbar')) {
        return;
    }

    const activeLink = sidebar.querySelector('a.active');
    const pageTitle = activeLink ? activeLink.textContent.trim() : 'PICO 2 IoT GUIDE';

    const topbar = document.createElement('div');
    topbar.className = 'mobile-topbar';
    topbar.innerHTML = [
        '<button class="mobile-menu-trigger" type="button" aria-label="메뉴 열기" aria-expanded="false">',
        '<i class="fa-solid fa-bars" aria-hidden="true"></i>',
        '</button>',
        '<div class="mobile-brand"></div>'
    ].join('');

    const edgeTab = document.createElement('button');
    edgeTab.className = 'mobile-edge-tab';
    edgeTab.type = 'button';
    edgeTab.setAttribute('aria-label', '왼쪽 메뉴 열기');
    edgeTab.innerHTML = '<i class="fa-solid fa-chevron-right" aria-hidden="true"></i>';

    const backdrop = document.createElement('div');
    backdrop.className = 'mobile-menu-backdrop';

    document.body.insertBefore(topbar, document.body.firstChild);
    document.body.appendChild(edgeTab);
    document.body.appendChild(backdrop);
    topbar.querySelector('.mobile-brand').textContent = pageTitle;

    const trigger = topbar.querySelector('.mobile-menu-trigger');
    const setOpen = (isOpen) => {
        body.classList.toggle('menu-open', isOpen);
        trigger.setAttribute('aria-expanded', String(isOpen));
        trigger.setAttribute('aria-label', isOpen ? '메뉴 닫기' : '메뉴 열기');
        trigger.innerHTML = isOpen
            ? '<i class="fa-solid fa-xmark" aria-hidden="true"></i>'
            : '<i class="fa-solid fa-bars" aria-hidden="true"></i>';
    };

    trigger.addEventListener('click', () => setOpen(!body.classList.contains('menu-open')));
    edgeTab.addEventListener('click', () => setOpen(true));
    backdrop.addEventListener('click', () => setOpen(false));

    sidebar.querySelectorAll('a').forEach((link) => {
        link.addEventListener('click', () => setOpen(false));
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            setOpen(false);
        }
    });
})();
