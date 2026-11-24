package dev.aqua.client.gui;

import dev.aqua.client.compat.V;
import dev.aqua.client.config.ConfigManager;
import dev.aqua.client.module.Category;
import dev.aqua.client.module.HudModule;
import dev.aqua.client.module.Module;
import dev.aqua.client.module.ModuleManager;
import dev.aqua.client.module.impl.Watermark;
import dev.aqua.client.setting.*;
import dev.aqua.client.util.Anim;
import dev.aqua.client.util.ColorUtil;
import dev.aqua.client.util.Keys;
import dev.aqua.client.util.Lang;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Главное меню Aqua Client (правый Shift) в стиле Lunar:
 * категории слева, карточки модулей, страница настроек, поиск, анимации и частицы портала.
 */
public class ClickGuiScreen extends Screen {
    private static Category lastCategory = null;     // null = «Все модули»
    private static final float SIDEBAR = 118, HEADER = 36, CARD_H = 60, GAP = 8;

    private record Hit(float x, float y, float w, float h, IntConsumer click, DoubleConsumer drag) {}

    private final List<Hit> hits = new ArrayList<>();
    private final Map<Object, Anim> anims = new HashMap<>();
    private final List<float[]> particles = new ArrayList<>(); // x, y, vx, vy, life, maxLife, size, colorIdx
    private final Random random = new Random();

    private final Anim open = new Anim(0);
    private final Anim catIndicator = new Anim(0);
    private final Anim scroll = new Anim(0);
    private final Anim page = new Anim(0);
    private final List<float[]> bursts = new ArrayList<>(); // x, y, life — вспышка при включении модуля
    private Module tipModule;
    private float tipTime;
    private float waveT, lastX0, lastY0;
    private float scrollTarget, contentHeight, viewHeight;
    private boolean closing;
    private long lastNs = System.nanoTime();
    private float dt, alpha;

    private Category category = lastCategory;
    private Module settingsModule;
    private Module bindingModule;
    private ColorSetting expandedColor;
    private StringSetting editingString;
    private boolean showProfiles;
    private final StringSetting profileBuffer = new StringSetting("profile", "", 18);
    private String renameTarget;          // имя при переименовании; "" = создание новой; null = не редактируем
    private Runnable profileCommit;
    private KeySetting bindingKeySetting;
    private DoubleConsumer activeDrag;
    private String search = "";

    public ClickGuiScreen() { this(null); }

    public ClickGuiScreen(Module openSettings) {
        super(Text.literal("Aqua Client"));
        if (openSettings != null) {
            settingsModule = openSettings;
            page.snap(1);
        }
    }

    // ===================================================================== RENDER
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // размытый фон игры
        long now = System.nanoTime();
        dt = Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;

        open.to(closing ? 0 : 1);
        open.update(dt, closing ? 16 : 9);
        alpha = open.get();
        if (closing && alpha < 0.03f) { if (client != null) client.setScreen(null); return; }

        hits.clear();
        float W = Math.min(width - 30, 560), H = Math.min(height - 30, 340);
        float x0 = (width - W) / 2f;
        float y0 = (height - H) / 2f + (1 - Anim.easeOutCubic(alpha)) * 26;
        lastX0 = x0;
        lastY0 = y0;

        // тень и панель
        Render2D.shadow(ctx, x0, y0, W, H, 8, 6, f(0x503EE6FF));
        Render2D.roundRect(ctx, x0, y0, W, H, 8, f(Theme.BG));
        ctx.enableScissor((int) x0, (int) y0, (int) (x0 + W), (int) (y0 + H));
        ctx.fillGradient((int) x0, (int) y0, (int) (x0 + W), (int) (y0 + H * 0.45f), f(0x223EE6FF), f(0x003EE6FF));
        renderWaves(ctx, x0, y0, W, H);
        renderParticles(ctx, x0, y0, W, H);
        ctx.disableScissor();
        Render2D.roundOutline(ctx, x0, y0, W, H, 8, f(Theme.BORDER));
        // мягкий светящийся контур при открытии
        if (alpha < 0.999f && !closing) {
            float e = Anim.easeOutCubic(alpha), grow = (1 - e) * 18;
            Render2D.roundOutline(ctx, x0 - grow, y0 - grow, W + grow * 2, H + grow * 2, 8 + grow, ColorUtil.withAlpha(0x3EE6FF, (int) (200 * (1 - e))));
        }
        // бегущий блик по верхней кромке панели
        float sweep = (System.currentTimeMillis() % 4000) / 4000f;
        float sx0 = x0 + 10 + (W - 60) * sweep;
        ctx.fillGradient((int) sx0, (int) y0, (int) sx0 + 40, (int) y0 + 1, f(0x003EE6FF), f(0xC09FF6FF));

        renderSidebar(ctx, mouseX, mouseY, x0, y0, H);
        renderFlags(ctx, mouseX, mouseY, x0, y0, H);

        float cx = x0 + SIDEBAR + 10, cw = W - SIDEBAR - 20;
        float cy = y0 + HEADER + 6, ch = H - HEADER - 18;
        page.to(settingsModule != null ? 1 : 0);
        page.update(dt, 12);
        renderHeader(ctx, mouseX, mouseY, cx, y0, cw);

        viewHeight = ch;
        scrollTarget = Math.max(0, Math.min(scrollTarget, Math.max(0, contentHeight - ch)));
        scroll.to(scrollTarget);
        scroll.update(dt, 16);

        ctx.enableScissor((int) cx - 2, (int) cy, (int) (cx + cw + 2), (int) (cy + ch));
        float p = page.get();
        Module tipBefore = tipModule;
        tipModule = null;
        if (p < 0.999f) {
            if (showProfiles) renderProfiles(ctx, mouseX, mouseY, cx - p * 40, cy, cw, ch, 1 - p);
            else renderGrid(ctx, mouseX, mouseY, cx - p * 40, cy, cw, ch, 1 - p);
        }
        if (tipModule != tipBefore) tipTime = 0;
        if (p > 0.001f && settingsModule != null) renderSettings(ctx, mouseX, mouseY, cx + (1 - p) * 40, cy, cw, ch, p);
        ctx.disableScissor();

        // полоса прокрутки
        if (contentHeight > ch) {
            float bh = Math.max(20, ch * ch / contentHeight);
            float by = cy + (ch - bh) * (scroll.get() / Math.max(1, contentHeight - ch));
            Render2D.roundRect(ctx, x0 + W - 6, by, 3, bh, 1.5f, f(0x803EE6FF));
        }

        // вспышки-кольца при включении модулей
        Iterator<float[]> bi = bursts.iterator();
        while (bi.hasNext()) {
            float[] b = bi.next();
            b[2] -= dt * 2.2f;
            if (b[2] <= 0) { bi.remove(); continue; }
            float r = (1 - b[2]) * 26;
            Render2D.roundOutline(ctx, b[0] - r, b[1] - r, r * 2, r * 2, r, f(ColorUtil.withAlpha(0x5FF2C8, (int) (220 * b[2]))));
            Render2D.roundOutline(ctx, b[0] - r * 0.6f, b[1] - r * 0.6f, r * 1.2f, r * 1.2f, r * 0.6f, f(ColorUtil.withAlpha(0x3EE6FF, (int) (160 * b[2]))));
        }

        // строка статуса снизу — без счётчиков, только подсказка
        String status = Lang.tr("ПКМ — настройки");
        Render2D.textScaled(ctx, status, cx, y0 + H - 9, 0.62f, t(0xFF4F7F96), false);

        // подсказка с полным описанием
        if (tipModule != null && settingsModule == null) {
            tipTime += dt;
            if (tipTime > 0.55f) {
                float ta = Math.min(1, (tipTime - 0.55f) * 6);
                String d = Lang.tr(tipModule.getDescription());
                List<net.minecraft.text.OrderedText> lines = textRenderer.wrapLines(net.minecraft.text.Text.literal(d), 170);
                int tw = textRenderer.getWidth(tipModule.getName());
                for (var l : lines) tw = Math.max(tw, textRenderer.getWidth(l));
                float tx = Math.min(mouseX + 10, width - tw - 14), ty = mouseY + 12 - (1 - ta) * 4;
                float th = lines.size() * 10 + 18;
                Render2D.shadow(ctx, tx, ty, tw + 12, th, 5, 3, f2(0x60000000, ta));
                Render2D.roundRect(ctx, tx, ty, tw + 12, th, 5, f2(0xF2041522, ta));
                Render2D.roundOutline(ctx, tx, ty, tw + 12, th, 5, f2(Theme.ACCENT_DARK, ta));
                ctx.drawText(textRenderer, tipModule.getName(), (int) tx + 6, (int) ty + 5, t2(Theme.ACCENT, ta), false);
                for (int i = 0; i < lines.size(); i++) ctx.drawText(textRenderer, lines.get(i), (int) tx + 6, (int) ty + 16 + i * 10, t2(Theme.TEXT, ta), false);
            }
        }
    }

    /** Две полупрозрачные волны, бегущие по верху панели. */
    private void renderWaves(DrawContext ctx, float x0, float y0, float W, float H) {
        waveT += dt;
        for (int layer = 0; layer < 2; layer++) {
            float speed = layer == 0 ? 1.4f : -1f, amp = layer == 0 ? 3 : 2, base = y0 + 34 + layer * 5;
            int col = layer == 0 ? 0x183EE6FF : 0x125FF2C8;
            for (int x = 0; x < W; x += 2) {
                float wy = base + (float) Math.sin(x * 0.035f + waveT * speed) * amp + (float) Math.sin(x * 0.011f - waveT * 0.6f) * amp;
                ctx.fill((int) (x0 + x), (int) y0, (int) (x0 + x + 2), (int) wy, f(col));
            }
        }
    }

    private void renderSidebar(DrawContext ctx, int mx, int my, float x0, float y0, float H) {
        Render2D.roundRect(ctx, x0, y0, SIDEBAR, H, 8, f(Theme.SIDEBAR));
        Render2D.rect(ctx, x0 + SIDEBAR - 1, y0 + 8, 1, H - 16, f(Theme.BORDER));

        // логотип: жемчуг Края + градиентная надпись
        dev.aqua.client.compat.V.push(ctx);
        dev.aqua.client.compat.V.translate(ctx, x0 + 10, y0 + 11);
        dev.aqua.client.compat.V.scale(ctx, 2);
        Watermark.drawTurtle(ctx, 0, 0);
        dev.aqua.client.compat.V.pop(ctx);
        Render2D.gradientText(ctx, "AQUA", x0 + 30, y0 + 10, 1.45f, Theme.ACCENT, Theme.EYE, true, alpha);
        Render2D.textScaled(ctx, "CLIENT  v2.0", x0 + 30, y0 + 23, 0.65f, t(Theme.SUB), false);

        List<Category> cats = new ArrayList<>();
        cats.add(null);
        cats.addAll(Arrays.asList(Category.values()));
        float rowY = y0 + 46, rowH = 22;
        int selectedIdx = cats.indexOf(category);
        catIndicator.to(selectedIdx);
        catIndicator.update(dt, 14);
        float iy = rowY + catIndicator.get() * rowH;
        Render2D.roundRect(ctx, x0 + 6, iy + 1, SIDEBAR - 12, rowH - 2, 4, f(0x403EE6FF));
        Render2D.roundRect(ctx, x0 + 6, iy + 5, 2, rowH - 10, 1, f(Theme.ACCENT));
        for (int i = 0; i < cats.size(); i++) {
            Category c = cats.get(i);
            float ry = rowY + i * rowH;
            boolean hov = Render2D.hover(mx, my, x0 + 6, ry, SIDEBAR - 12, rowH);
            Anim h = anim("cat" + i, hov ? 1 : 0, 14);
            if (h.get() > 0.01f && i != selectedIdx) Render2D.roundRect(ctx, x0 + 6, ry + 1, SIDEBAR - 12, rowH - 2, 4, f(ColorUtil.withAlpha(0x3EE6FF, (int) (40 * h.get()))));
            String icon = c == null ? "◆" : c.icon;
            String name = c == null ? Lang.tr("Все модули") : Lang.tr(c.title);
            int col = i == selectedIdx ? Theme.TEXT : ColorUtil.lerp(Theme.SUB, Theme.TEXT, h.get());
            ctx.drawText(textRenderer, icon, (int) (x0 + 14 + h.get() * 2), (int) (ry + 7), t(i == selectedIdx ? Theme.ACCENT : col), false);
            ctx.drawText(textRenderer, name, (int) (x0 + 28 + h.get() * 2), (int) (ry + 7), t(col), false);
            final Category target = c;
            hit(x0 + 6, ry, SIDEBAR - 12, rowH, b -> {
                category = target;
                lastCategory = target;
                showProfiles = false;
                editingString = null;
                settingsModule = null;
                expandedColor = null;
                search = "";
                scrollTarget = 0;
            });
        }

        // рыбка плавает внизу боковой панели
        float fy = y0 + H - 82 + (float) Math.sin(System.currentTimeMillis() / 600.0) * 2;
        float fx = x0 + 14 + (float) ((System.currentTimeMillis() / 25) % (long) (SIDEBAR - 34));
        Render2D.rect(ctx, fx, fy, 8, 4, f(0xFFFF8A3D));
        Render2D.rect(ctx, fx + 2, fy, 2, 4, f(0xFFFFFFFF));
        Render2D.rect(ctx, fx - 3, fy - 1, 3, 6, f(0xFFE87430));
        Render2D.rect(ctx, fx + 6, fy + 1, 1, 1, f(0xFF10202A));

        // нижние кнопки: Сборки и Редактор HUD (в ряд)
        float by = y0 + H - 34, bh = 24, half = (SIDEBAR - 16 - 6) / 2f, lx = x0 + 8, rx = lx + half + 6;
        boolean phov = Render2D.hover(mx, my, lx, by, half, bh);
        Anim ph = anim("profbtn", phov || showProfiles ? 1 : 0, 14);
        Render2D.roundRect(ctx, lx, by, half, bh, 5, f(ColorUtil.lerp(Theme.ACCENT_DARK, Theme.ACCENT, ph.get())));
        Render2D.centered(ctx, Lang.tr("Сборки"), lx + half / 2, by + 8, t(0xFFFFFFFF));
        hit(lx, by, half, bh, b -> { showProfiles = !showProfiles; settingsModule = null; editingString = null; profileCommit = null; renameTarget = null; scrollTarget = 0; });

        boolean hov = Render2D.hover(mx, my, rx, by, half, bh);
        Anim h = anim("hudbtn", hov ? 1 : 0, 14);
        Render2D.roundRect(ctx, rx, by, half, bh, 5, f(ColorUtil.lerp(Theme.ACCENT_DARK, Theme.ACCENT, h.get())));
        Render2D.centered(ctx, Lang.tr("HUD"), rx + half / 2, by + 8, t(0xFFFFFFFF));
        hit(rx, by, half, bh, b -> { if (client != null) client.setScreen(new HudEditorScreen()); });
    }

    private void renderHeader(DrawContext ctx, int mx, int my, float cx, float y0, float cw) {
        float hy = y0 + 12;
        if (settingsModule != null) {
            boolean hov = Render2D.hover(mx, my, cx, hy - 3, 62, 16);
            Anim h = anim("back", hov ? 1 : 0, 14);
            Render2D.roundRect(ctx, cx, hy - 3, 62, 16, 4, f(ColorUtil.lerp(Theme.CARD, Theme.CARD_HOVER, h.get())));
            ctx.drawText(textRenderer, Lang.tr("‹ Назад"), (int) cx + 8, (int) hy + 1, t(Theme.TEXT), false);
            hit(cx, hy - 3, 62, 16, b -> closeSettings());
            Render2D.textScaled(ctx, settingsModule.getName(), cx + 72, hy - 1, 1.25f, t(Theme.TEXT), true);
        } else {
            String title = showProfiles ? Lang.tr("Сборки") : category == null ? Lang.tr("Все модули") : Lang.tr(category.title);
            if (!search.isEmpty() && !showProfiles) title = Lang.tr("Поиск");
            Render2D.textScaled(ctx, title, cx, hy - 1, 1.25f, t(Theme.TEXT), true);
            if (showProfiles) return;
            // поиск
            float sw = Math.min(150, cw * 0.4f), sx = cx + cw - sw, sy = hy - 4;
            Render2D.roundRect(ctx, sx, sy, sw, 17, 5, f(Theme.CARD));
            Render2D.roundOutline(ctx, sx, sy, sw, 17, 5, f(search.isEmpty() ? Theme.BORDER : Theme.ACCENT));
            boolean cursor = (System.currentTimeMillis() / 500) % 2 == 0;
            String shown = search.isEmpty() ? Lang.tr("⌕ Поиск...") : textRenderer.trimToWidth(search, (int) sw - 16) + (cursor ? "_" : "");
            ctx.drawText(textRenderer, shown, (int) sx + 6, (int) sy + 5, t(search.isEmpty() ? 0xFF5A88A0 : Theme.TEXT), false);
        }
    }

    /** Два флажка снизу сайдбара — переключение языка клиента (RU / EN). */
    private void renderFlags(DrawContext ctx, int mx, int my, float x0, float y0, float H) {
        float fw = 26, fh = 16, gp = 8, fy = y0 + H - 34 - fh - 8;
        float ruX = x0 + 12, enX = ruX + fw + gp;
        boolean ru = Lang.isRu();
        // РФ — три полосы
        int rx = (int) ruX, ry = (int) fy, rx2 = (int) (ruX + fw); float th = fh / 3f;
        ctx.fill(rx, ry, rx2, (int) (fy + th), 0xFFFFFFFF);
        ctx.fill(rx, (int) (fy + th), rx2, (int) (fy + 2 * th), 0xFF0039A6);
        ctx.fill(rx, (int) (fy + 2 * th), rx2, (int) (fy + fh), 0xFFD52B1E);
        Render2D.roundOutline(ctx, ruX - 1, fy - 1, fw + 2, fh + 2, 3, f(ru ? Theme.ACCENT : (Render2D.hover(mx, my, ruX, fy, fw, fh) ? Theme.TEXT : Theme.BORDER)));
        hit(ruX, fy, fw, fh, b -> { Lang.setLang(1); ConfigManager.save(); });
        // Англия — белый с красным крестом
        int ex = (int) enX, ey = (int) fy, ex2 = (int) (enX + fw), ey2 = (int) (fy + fh);
        ctx.fill(ex, ey, ex2, ey2, 0xFFFFFFFF);
        int ecx = (int) (enX + fw / 2), ecy = (int) (fy + fh / 2);
        ctx.fill(ecx - 2, ey, ecx + 2, ey2, 0xFFCF142B);
        ctx.fill(ex, ecy - 2, ex2, ecy + 2, 0xFFCF142B);
        Render2D.roundOutline(ctx, enX - 1, fy - 1, fw + 2, fh + 2, 3, f(!ru ? Theme.ACCENT : (Render2D.hover(mx, my, enX, fy, fw, fh) ? Theme.TEXT : Theme.BORDER)));
        hit(enX, fy, fw, fh, b -> { Lang.setLang(2); ConfigManager.save(); });
    }

    private void renderGrid(DrawContext ctx, int mx, int my, float x, float y, float w, float h, float a) {
        List<Module> list = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : (search.isEmpty() ? ModuleManager.byCategory(category) : ModuleManager.all())) {
            if (q.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(q) || Lang.tr(m.getDescription()).toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) list.add(m);
        }
        int cols = w >= 420 ? 3 : 2;
        float cardW = (w - GAP * (cols - 1)) / cols;
        int rows = (list.size() + cols - 1) / cols;
        if (settingsModule == null) contentHeight = rows * (CARD_H + GAP);
        float sc = scroll.get();

        if (list.isEmpty()) Render2D.centered(ctx, Lang.tr("Ничего не найдено"), x + w / 2, y + 30, t2(Theme.SUB, a));

        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int col = i % cols, row = i / cols;
            float cx = x + col * (cardW + GAP);
            float cy = y + row * (CARD_H + GAP) - sc;
            if (cy + CARD_H < y || cy > y + h) continue;
            // каскадное появление карточек
            Anim appear = anim(m.getName() + "#appear", 1, 10 - Math.min(6, i * 0.25f));
            float ap = appear.get() * a;
            float oy = (1 - appear.get()) * 10;
            cy += oy;

            boolean inView = my >= y && my <= y + h;
            boolean hov = inView && settingsModule == null && Render2D.hover(mx, my, cx, cy, cardW, CARD_H);
            Anim hv = anim(m.getName() + "#hover", hov ? 1 : 0, 14);
            Anim on = anim(m.getName() + "#on", m.isEnabled() ? 1 : 0, 12);

            int bg = ColorUtil.lerp(ColorUtil.lerp(Theme.CARD, Theme.CARD_ON, on.get() * 0.6f), Theme.CARD_HOVER, hv.get());
            Render2D.roundRect(ctx, cx, cy - hv.get() * 1.5f, cardW, CARD_H, 6, f2(bg, ap));
            if (on.get() > 0.01f) Render2D.roundOutline(ctx, cx, cy - hv.get() * 1.5f, cardW, CARD_H, 6, f2(ColorUtil.withAlpha(0x3EE6FF, (int) (150 * on.get())), ap));
            float top = cy - hv.get() * 1.5f;
            if (hov) tipModule = m;
            // блик, пробегающий по карточке при наведении
            if (hv.get() > 0.02f) {
                float sh = anim(m.getName() + "#shine", hov ? 1 : 0, 3.2f).get();
                float bx0 = cx + 4 + (cardW - 18) * sh;
                ctx.fillGradient((int) bx0, (int) top + 2, (int) bx0 + 10, (int) (top + CARD_H - 2), f2(ColorUtil.withAlpha(0xFFFFFF, (int) (26 * hv.get())), ap), f2(0x00FFFFFF, ap));
            }
            // пульсирующая точка у включённых модулей
            if (on.get() > 0.05f) {
                float pulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 300.0 + i);
                Render2D.circle(ctx, cx + cardW - 8, top + 8, 3.2f * on.get(), f2(ColorUtil.withAlpha(0x5FF2C8, (int) (70 * pulse)), ap));
                Render2D.circle(ctx, cx + cardW - 8, top + 8, 1.6f * on.get(), f2(Theme.ON, ap));
            }

            // иконка категории
            Render2D.roundRect(ctx, cx + 7, top + 7, 16, 16, 4, f2(ColorUtil.lerp(0xFF0E3350, Theme.ACCENT_DARK, on.get()), ap));
            Render2D.centered(ctx, m.getCategory().icon, cx + 15, top + 11, t2(ColorUtil.lerp(Theme.SUB, 0xFFFFFFFF, on.get()), ap));
            ctx.drawText(textRenderer, textRenderer.trimToWidth(m.getName(), (int) cardW - 34), (int) (cx + 28), (int) (top + 7), t2(Theme.TEXT, ap), false);
            String desc = Lang.tr(m.getDescription());
            if (textRenderer.getWidth(desc) * 0.75f > cardW - 34) desc = textRenderer.trimToWidth(desc, (int) ((cardW - 40) / 0.75f)) + "…";
            Render2D.textScaled(ctx, desc, cx + 28, top + 18, 0.75f, t2(Theme.SUB, ap), false);

            // кнопка вкл/выкл
            boolean hasSettings = !m.getSettings().isEmpty() || m instanceof HudModule;
            float bw = cardW - 14 - (hasSettings ? 22 : 0), bx = cx + 7, by = top + CARD_H - 23;
            int pill = ColorUtil.lerp(0xFF4A1A26, 0xFF0E4A44, on.get());
            Render2D.roundRect(ctx, bx, by, bw, 16, 4, f2(pill, ap));
            Render2D.roundRect(ctx, bx, by, bw * on.get(), 16, 4, f2(ColorUtil.withAlpha(0x5FF2C8, (int) (70 * on.get())), ap));
            String st = m.isEnabled() ? Lang.tr("ВКЛЮЧЁН") : Lang.tr("ВЫКЛЮЧЕН");
            Render2D.centered(ctx, st, bx + bw / 2, by + 4, t2(ColorUtil.lerp(Theme.OFF, Theme.ON, on.get()), ap));
            final float burstX = bx + bw / 2, burstY = by + 8;
            if (inView) hit(bx, by, bw, 16, b -> { if (b == 1) openSettings(m); else toggleFx(m, burstX, burstY); });

            if (hasSettings) {
                float gx = cx + cardW - 25;
                boolean gh = inView && Render2D.hover(mx, my, gx, by, 18, 16);
                Render2D.roundRect(ctx, gx, by, 18, 16, 4, f2(gh ? Theme.ACCENT_DARK : 0xFF0E3350, ap));
                Render2D.centered(ctx, "⚙", gx + 9.5f, by + 4, t2(Theme.TEXT, ap));
                if (inView) hit(gx, by, 18, 16, b -> openSettings(m));
            }
            final float cardMidX = cx + cardW / 2, cardMidY = top + CARD_H / 2;
            if (inView) hit(cx, top, cardW, CARD_H - 24, b -> { if (b == 1 && hasSettings) openSettings(m); else toggleFx(m, cardMidX, cardMidY); });
        }
    }

    // ===================================================================== SETTINGS PAGE
    /** Панель сборок (пресетов): список, применить/переименовать/удалить, сохранить текущую. */
    private void renderProfiles(DrawContext ctx, int mx, int my, float x, float y, float w, float h, float a) {
        java.util.List<String> names = dev.aqua.client.config.Profiles.names();
        float rowH = 30, gap = 6, sc = scroll.get(), yy = y - sc;
        Render2D.textScaled(ctx, Lang.tr("Сохрани включённые модули и настройки. До 5 сборок."), x, yy, 0.85f, t2(Theme.SUB, a), false);
        yy += 16;

        for (String name : names) {
            boolean active = name.equals(dev.aqua.client.config.Profiles.active());
            boolean editing = editingString == profileBuffer && name.equals(renameTarget);
            boolean hov = Render2D.hover(mx, my, x, yy, w - 52, rowH) && !editing;
            Anim rh = anim("prof" + name, hov ? 1 : 0, 14);
            if (yy + rowH > y - rowH && yy < y + h) {
                Render2D.roundRect(ctx, x, yy, w, rowH, 6, f(ColorUtil.lerp(active ? Theme.ACCENT_DARK : Theme.CARD, Theme.CARD_HOVER, rh.get())));
                if (active) Render2D.roundOutline(ctx, x, yy, w, rowH, 6, f(Theme.ACCENT));
                if (editing) {
                    boolean cur = (System.currentTimeMillis() / 500) % 2 == 0;
                    ctx.drawText(textRenderer, profileBuffer.get() + (cur ? "_" : ""), (int) x + 10, (int) yy + 11, t(Theme.TEXT), false);
                } else {
                    ctx.drawText(textRenderer, name, (int) x + 10, (int) yy + 7, t(Theme.TEXT), false);
                    ctx.drawText(textRenderer, Lang.tr(active ? "активна · нажми, чтобы применить" : "нажми, чтобы применить"), (int) x + 10, (int) yy + 18, t2(Theme.SUB, a), false);
                }
                // переименовать
                Render2D.centered(ctx, "✎", x + w - 40, yy + rowH / 2 - 4, t(Render2D.hover(mx, my, x + w - 52, yy, 26, rowH) ? Theme.TEXT : Theme.SUB));
                // удалить
                Render2D.centered(ctx, "✕", x + w - 14, yy + rowH / 2 - 4, t(Render2D.hover(mx, my, x + w - 26, yy, 26, rowH) ? 0xFFFF5566 : Theme.SUB));
            }
            final String fn = name;
            if (!editing) hit(x, yy, w - 52, rowH, b -> dev.aqua.client.config.Profiles.apply(fn));
            hit(x + w - 52, yy, 26, rowH, b -> startRenameProfile(fn));
            hit(x + w - 26, yy, 26, rowH, b -> dev.aqua.client.config.Profiles.delete(fn));
            yy += rowH + gap;
        }

        // строка создания новой сборки
        if (editingString == profileBuffer && "".equals(renameTarget)) {
            Render2D.roundRect(ctx, x, yy, w, rowH, 6, f(Theme.CARD));
            Render2D.roundOutline(ctx, x, yy, w, rowH, 6, f(Theme.ACCENT));
            boolean cur = (System.currentTimeMillis() / 500) % 2 == 0;
            String shown = profileBuffer.get().isEmpty() ? Lang.tr("Имя сборки...") : profileBuffer.get();
            ctx.drawText(textRenderer, shown + (cur ? "_" : ""), (int) x + 10, (int) yy + 11, t(profileBuffer.get().isEmpty() ? 0xFF5A88A0 : Theme.TEXT), false);
            yy += rowH + gap;
        } else if (dev.aqua.client.config.Profiles.canAdd()) {
            boolean hov = Render2D.hover(mx, my, x, yy, w, 26);
            Anim bh = anim("profadd", hov ? 1 : 0, 14);
            Render2D.roundRect(ctx, x, yy, w, 26, 6, f(ColorUtil.lerp(Theme.CARD, Theme.ACCENT_DARK, bh.get())));
            Render2D.centered(ctx, Lang.tr("＋ Сохранить текущую сборку"), x + w / 2, yy + 9, t(Theme.TEXT));
            hit(x, yy, w, 26, b -> startCreateProfile());
            yy += 26 + gap;
        } else {
            Render2D.centered(ctx, Lang.tr("Достигнут максимум — 5 сборок"), x + w / 2, yy + 9, t2(Theme.SUB, a));
            yy += 26 + gap;
        }

        // обновить активную сборку текущим состоянием
        if (!dev.aqua.client.config.Profiles.active().isEmpty() && names.contains(dev.aqua.client.config.Profiles.active()) && !(editingString == profileBuffer)) {
            boolean hov = Render2D.hover(mx, my, x, yy, w, 24);
            Anim bh = anim("profupd", hov ? 1 : 0, 14);
            Render2D.roundRect(ctx, x, yy, w, 24, 6, f(ColorUtil.lerp(Theme.CARD, Theme.CARD_HOVER, bh.get())));
            Render2D.centered(ctx, Lang.tr("⟳ Обновить «") + dev.aqua.client.config.Profiles.active() + "»", x + w / 2, yy + 8, t(Theme.SUB));
            hit(x, yy, w, 24, b -> dev.aqua.client.config.Profiles.saveCurrent(dev.aqua.client.config.Profiles.active()));
            yy += 24 + gap;
        }

        // ---- обмен конфигом по коду: скопировать свой / вставить чужой ----
        {
            boolean hov = Render2D.hover(mx, my, x, yy, w, 24);
            Anim bh = anim("profcopy", hov ? 1 : 0, 14);
            Render2D.roundRect(ctx, x, yy, w, 24, 6, f(ColorUtil.lerp(Theme.CARD, Theme.ACCENT_DARK, bh.get())));
            Render2D.centered(ctx, Lang.tr("⧉ Скопировать мой код"), x + w / 2, yy + 8, t(Theme.TEXT));
            hit(x, yy, w, 24, b -> {
                if (client != null) client.keyboard.setClipboard(dev.aqua.client.config.Profiles.exportCurrent());
                toast(Lang.tr("Код скопирован — поделись с другом"));
            });
            yy += 24 + gap;
        }
        {
            boolean hov = Render2D.hover(mx, my, x, yy, w, 24);
            Anim bh = anim("profpaste", hov ? 1 : 0, 14);
            Render2D.roundRect(ctx, x, yy, w, 24, 6, f(ColorUtil.lerp(Theme.CARD, Theme.CARD_HOVER, bh.get())));
            Render2D.centered(ctx, Lang.tr("📥 Вставить код сборки"), x + w / 2, yy + 8, t(Theme.SUB));
            hit(x, yy, w, 24, b -> {
                String fn = client == null ? null : dev.aqua.client.config.Profiles.importCode(client.keyboard.getClipboard(), null);
                toast(fn != null ? Lang.tr("Сборка добавлена: ") + fn : Lang.tr("Неверный код или достигнут максимум сборок"));
            });
            yy += 24 + gap;
        }
        contentHeight = yy + sc - y + 6;
    }

    private void toast(String msg) {
        if (client != null && client.player != null)
            client.player.sendMessage(net.minecraft.text.Text.literal("§b[Aqua] §f" + msg), false);
    }

    private void startRenameProfile(String name) {
        renameTarget = name;
        profileBuffer.set(name);
        editingString = profileBuffer;
        profileCommit = () -> dev.aqua.client.config.Profiles.rename(name, profileBuffer.get());
        bindingKeySetting = null;
    }

    private void startCreateProfile() {
        renameTarget = "";
        profileBuffer.set("");
        editingString = profileBuffer;
        profileCommit = () -> dev.aqua.client.config.Profiles.saveCurrent(profileBuffer.get());
        bindingKeySetting = null;
    }

    private void renderSettings(DrawContext ctx, int mx, int my, float x, float y, float w, float h, float a) {
        Module m = settingsModule;
        float sc = scroll.get();
        float ry = y - sc;
        boolean inView = my >= y && my <= y + h;

        Render2D.textScaled(ctx, Lang.tr(m.getDescription()), x + 2, ry + 2, 0.85f, t2(Theme.SUB, a), false);
        ry += 16;

        ry = rowBool(ctx, mx, my, x, ry, w, Lang.tr("Модуль включён"), m.isEnabled(), m::toggle, m.getName() + "#enabled", a, inView);
        // клавиша
        rowFrame(ctx, x, ry, w, a);
        ctx.drawText(textRenderer, Lang.tr("Клавиша"), (int) x + 8, (int) ry + 8, t2(Theme.TEXT, a), false);
        String key = bindingModule == m ? Lang.tr("Нажмите клавишу или кнопку мыши...") : Keys.name(m.getKey());
        float kw = Math.max(60, textRenderer.getWidth(key) + 16), kx = x + w - kw - 8;
        boolean kh = inView && Render2D.hover(mx, my, kx, ry + 4, kw, 16);
        Render2D.roundRect(ctx, kx, ry + 4, kw, 16, 4, f2(bindingModule == m ? Theme.ACCENT_DARK : (kh ? Theme.CARD_HOVER : 0xFF0E3350), a));
        Render2D.centered(ctx, key, kx + kw / 2, ry + 8, t2(Theme.TEXT, a));
        if (inView) hit(kx, ry + 4, kw, 16, b -> bindingModule = (bindingModule == m ? null : m));
        ry += 26;
        ry = rowBool(ctx, mx, my, x, ry, w, Lang.tr("Работает только пока зажата клавиша"), m.isHoldKey(), () -> { m.setHoldKey(!m.isHoldKey()); ConfigManager.save(); }, m.getName() + "#hold", a, inView);

        if (m instanceof HudModule hud) {
            rowFrame(ctx, x, ry, w, a);
            ctx.drawText(textRenderer, Lang.tr("Позиция на экране"), (int) x + 8, (int) ry + 8, t2(Theme.TEXT, a), false);
            float bw = 70, bx = x + w - bw - 8, bw2 = 80, bx2 = bx - bw2 - 6;
            button(ctx, mx, my, bx, ry + 4, bw, Lang.tr("Сбросить"), a, inView, () -> hud.resetPosition());
            button(ctx, mx, my, bx2, ry + 4, bw2, Lang.tr("Редактор HUD"), a, inView, () -> { if (client != null) client.setScreen(new HudEditorScreen()); });
            ry += 26;
        }

        // живое превью косметики — своя модель с выбранным гардеробом, обновляется сразу
        if (m instanceof dev.aqua.client.module.impl.Cosmetics && client != null && client.player != null) {
            float ph = 118;
            Render2D.roundRect(ctx, x, ry, w, ph, 6, f2(Theme.CARD, a));
            Render2D.textScaled(ctx, Lang.tr("Превью"), x + 8, ry + 5, 0.8f, t2(Theme.SUB, a), false);
            int cxp = (int) (x + w / 2);
            try {
                dev.aqua.client.compat.V.drawEntityPreview(ctx, cxp - 30, (int) (ry + 18), cxp + 30, (int) (ry + ph - 8), 46, mx, my, client.player);
            } catch (Throwable ignored) {}
            ry += ph + 6;
        }

        ry += 4;
        for (Setting<?> s : m.getSettings()) {
            if (!s.isVisible()) continue;
            if (s instanceof BoolSetting b) ry = rowBool(ctx, mx, my, x, ry, w, b.getName(), b.isOn(), b::toggle, b, a, inView);
            else if (s instanceof NumberSetting n) ry = rowNumber(ctx, mx, my, x, ry, w, n, a, inView);
            else if (s instanceof ModeSetting md) ry = rowMode(ctx, mx, my, x, ry, w, md, a, inView);
            else if (s instanceof ColorSetting c) ry = rowColor(ctx, mx, my, x, ry, w, c, a, inView);
            else if (s instanceof StringSetting st) ry = rowString(ctx, mx, my, x, ry, w, st, a, inView);
            else if (s instanceof KeySetting ks) ry = rowKey(ctx, mx, my, x, ry, w, ks, a, inView);
            else if (s instanceof ButtonSetting bs) ry = rowButton(ctx, mx, my, x, ry, w, bs, a, inView);
        }
        contentHeight = ry + sc - y + 6;
    }

    private void rowFrame(DrawContext ctx, float x, float y, float w, float a) {
        Render2D.roundRect(ctx, x, y, w, 24, 5, f2(Theme.CARD, a));
    }

    private float rowBool(DrawContext ctx, int mx, int my, float x, float y, float w, String label, boolean value, Runnable toggle, Object key, float a, boolean inView) {
        rowFrame(ctx, x, y, w, a);
        ctx.drawText(textRenderer, Lang.tr(label), (int) x + 8, (int) y + 8, t2(Theme.TEXT, a), false);
        Anim an = anim(key, value ? 1 : 0, 14);
        float sx = x + w - 34, sy = y + 6;
        Render2D.roundRect(ctx, sx, sy, 26, 12, 6, f2(ColorUtil.lerp(0xFF1E4560, Theme.ACCENT, an.get()), a));
        Render2D.circle(ctx, sx + 6 + an.get() * 14, sy + 6, 4.5f, f2(0xFFFFFFFF, a));
        if (inView) hit(x, y, w, 24, b -> toggle.run());
        return y + 26;
    }

    private float rowNumber(DrawContext ctx, int mx, int my, float x, float y, float w, NumberSetting n, float a, boolean inView) {
        rowFrame(ctx, x, y, w, a);
        ctx.drawText(textRenderer, Lang.tr(n.getName()), (int) x + 8, (int) y + 8, t2(Theme.TEXT, a), false);
        float sw = Math.min(150, w * 0.42f), sx = x + w - sw - 10, sy = y + 10;
        String val = n.display();
        ctx.drawText(textRenderer, val, (int) (sx - textRenderer.getWidth(val) - 8), (int) y + 8, t2(Theme.EYE, a), false);
        Anim an = anim(n, (float) n.percent(), 18);
        Render2D.roundRect(ctx, sx, sy, sw, 4, 2, f2(0xFF1E4560, a));
        Render2D.roundRect(ctx, sx, sy, Math.max(4, sw * an.get()), 4, 2, f2(Theme.ACCENT, a));
        Render2D.circle(ctx, sx + sw * an.get(), sy + 2, 4.5f, f2(0xFFFFFFFF, a));
        if (inView) hitDrag(sx - 4, y, sw + 8, 24, mxv -> n.setPercent((mxv - sx) / sw));
        return y + 26;
    }

    private float rowMode(DrawContext ctx, int mx, int my, float x, float y, float w, ModeSetting md, float a, boolean inView) {
        rowFrame(ctx, x, y, w, a);
        ctx.drawText(textRenderer, Lang.tr(md.getName()), (int) x + 8, (int) y + 8, t2(Theme.TEXT, a), false);
        float bw = 110, bx = x + w - bw - 8, by = y + 4;
        boolean hov = inView && Render2D.hover(mx, my, bx, by, bw, 16);
        Render2D.roundRect(ctx, bx, by, bw, 16, 4, f2(hov ? Theme.CARD_HOVER : 0xFF0E3350, a));
        ctx.drawText(textRenderer, "‹", (int) bx + 5, (int) by + 4, t2(Theme.ACCENT, a), false);
        ctx.drawText(textRenderer, "›", (int) (bx + bw - 9), (int) by + 4, t2(Theme.ACCENT, a), false);
        Render2D.centered(ctx, textRenderer.trimToWidth(md.display(), (int) bw - 24), bx + bw / 2, by + 4, t2(Theme.TEXT, a));
        if (inView) {
            hit(bx, by, bw / 2, 16, b -> md.cycle(-1));
            hit(bx + bw / 2, by, bw / 2, 16, b -> md.cycle(1));
        }
        return y + 26;
    }

    private float rowColor(DrawContext ctx, int mx, int my, float x, float y, float w, ColorSetting c, float a, boolean inView) {
        boolean expanded = expandedColor == c;
        Anim ex = anim(c, expanded ? 1 : 0, 14);
        float extra = 104 * ex.get();
        Render2D.roundRect(ctx, x, y, w, 24 + extra, 5, f2(Theme.CARD, a));
        ctx.drawText(textRenderer, Lang.tr(c.getName()), (int) x + 8, (int) y + 8, t2(Theme.TEXT, a), false);
        float sx = x + w - 34;
        // «шахматка» под прозрачным цветом
        Render2D.roundRect(ctx, sx, y + 6, 26, 12, 3, f2(0xFF808080, a));
        Render2D.rect(ctx, sx + 2, y + 8, 6, 4, f2(0xFFC0C0C0, a));
        Render2D.rect(ctx, sx + 14, y + 12, 6, 4, f2(0xFFC0C0C0, a));
        Render2D.roundRect(ctx, sx, y + 6, 26, 12, 3, f2(c.color(), a));
        if (c.isChroma()) Render2D.textScaled(ctx, "RGB", sx - 22, y + 9, 0.7f, t2(Theme.EYE, a), false);
        if (inView) hit(x, y, w, 24, b -> expandedColor = expanded ? null : c);

        if (ex.get() > 0.02f) {
            float[] hsb = ColorUtil.toHsb(c.get());
            int alphaV = ColorUtil.alpha(c.get());
            float py = y + 26;
            String[] labels = {Lang.tr("Оттенок"), Lang.tr("Насыщенность"), Lang.tr("Яркость"), Lang.tr("Прозрачность")};
            float sw = w - 120, slx = x + 104;
            for (int i = 0; i < 4; i++) {
                float ly = py + i * 18;
                if (ly + 10 > y + 24 + extra) break;
                Render2D.textScaled(ctx, labels[i], x + 10, ly + 3, 0.8f, t2(Theme.SUB, a * ex.get()), false);
                if (i == 0) {
                    for (int k = 0; k < 36; k++) Render2D.rect(ctx, slx + sw * k / 36f, ly + 3, sw / 36f + 1, 6, f2(ColorUtil.fromHsb(k / 36f, 1, 1, 255), a * ex.get()));
                } else {
                    int from = i == 1 ? ColorUtil.fromHsb(hsb[0], 0, hsb[2], 255) : i == 2 ? 0xFF000000 : ColorUtil.withAlpha(c.get(), 0);
                    int to = i == 1 ? ColorUtil.fromHsb(hsb[0], 1, hsb[2], 255) : i == 2 ? ColorUtil.fromHsb(hsb[0], hsb[1], 1, 255) : ColorUtil.withAlpha(c.get(), 255);
                    for (int k = 0; k < 24; k++) Render2D.rect(ctx, slx + sw * k / 24f, ly + 3, sw / 24f + 1, 6, f2(ColorUtil.lerp(from, to, k / 23f), a * ex.get()));
                }
                float v = i == 0 ? hsb[0] : i == 1 ? hsb[1] : i == 2 ? hsb[2] : alphaV / 255f;
                Render2D.circle(ctx, slx + sw * v, ly + 6, 4.5f, f2(0xFFFFFFFF, a * ex.get()));
                final int idx = i;
                if (inView && expanded) hitDrag(slx - 4, ly, sw + 8, 14, mxv -> {
                    float p = (float) Math.max(0, Math.min(1, (mxv - slx) / sw));
                    float[] cur = ColorUtil.toHsb(c.get());
                    int al = ColorUtil.alpha(c.get());
                    if (idx == 0) cur[0] = Math.min(0.999f, p);
                    if (idx == 1) cur[1] = p;
                    if (idx == 2) cur[2] = p;
                    if (idx == 3) al = Math.round(p * 255);
                    c.set(ColorUtil.fromHsb(cur[0], cur[1], cur[2], al));
                });
            }
            float cy = py + 74;
            if (cy + 12 <= y + 24 + extra) {
                Render2D.textScaled(ctx, Lang.tr("Хрома (радуга)"), x + 10, cy + 3, 0.8f, t2(Theme.SUB, a * ex.get()), false);
                Anim ch = anim(c.getName() + c.hashCode() + "#chroma", c.isChroma() ? 1 : 0, 14);
                float tx = x + w - 34;
                Render2D.roundRect(ctx, tx, cy, 26, 12, 6, f2(ColorUtil.lerp(0xFF1E4560, Theme.EYE, ch.get()), a * ex.get()));
                Render2D.circle(ctx, tx + 6 + ch.get() * 14, cy + 6, 4.5f, f2(0xFFFFFFFF, a * ex.get()));
                if (inView && expanded) hit(tx - 4, cy - 2, 34, 16, b -> c.setChroma(!c.isChroma()));
            }
        }
        return y + 26 + extra;
    }

    private float rowString(DrawContext ctx, int mx, int my, float x, float y, float w, StringSetting st, float a, boolean inView) {
        rowFrame(ctx, x, y, w, a);
        ctx.drawText(textRenderer, Lang.tr(st.getName()), (int) x + 8, (int) y + 8, t2(Theme.TEXT, a), false);
        float bw = Math.min(190, w * 0.55f), bx = x + w - bw - 8, by = y + 4;
        boolean editing = editingString == st;
        Render2D.roundRect(ctx, bx, by, bw, 16, 4, f2(editing ? 0xFF0E4664 : 0xFF0E3350, a));
        Render2D.roundOutline(ctx, bx, by, bw, 16, 4, f2(editing ? Theme.ACCENT : Theme.BORDER, a));
        String shown = st.get().isEmpty() && !editing ? Lang.tr("нажмите, чтобы ввести") : st.get();
        String tail = textRenderer.trimToWidth(shown, (int) bw - 14, true);
        boolean cursor = editing && (System.currentTimeMillis() / 500) % 2 == 0;
        ctx.drawText(textRenderer, tail + (cursor ? "_" : ""), (int) bx + 5, (int) by + 4, t2(st.get().isEmpty() && !editing ? 0xFF5A88A0 : Theme.TEXT, a), false);
        if (inView) hit(bx, by, bw, 16, b -> { editingString = editing ? null : st; bindingKeySetting = null; });
        return y + 26;
    }

    private float rowKey(DrawContext ctx, int mx, int my, float x, float y, float w, KeySetting ks, float a, boolean inView) {
        rowFrame(ctx, x, y, w, a);
        ctx.drawText(textRenderer, Lang.tr(ks.getName()), (int) x + 8, (int) y + 8, t2(Theme.TEXT, a), false);
        String key = bindingKeySetting == ks ? Lang.tr("Нажмите клавишу...") : Keys.name(ks.get());
        float kw = Math.max(60, textRenderer.getWidth(key) + 16), kx = x + w - kw - 8;
        boolean kh = inView && Render2D.hover(mx, my, kx, y + 4, kw, 16);
        Render2D.roundRect(ctx, kx, y + 4, kw, 16, 4, f2(bindingKeySetting == ks ? Theme.ACCENT_DARK : (kh ? Theme.CARD_HOVER : 0xFF0E3350), a));
        Render2D.centered(ctx, key, kx + kw / 2, y + 8, t2(Theme.TEXT, a));
        if (inView) hit(kx, y + 4, kw, 16, b -> { bindingKeySetting = bindingKeySetting == ks ? null : ks; editingString = null; });
        return y + 26;
    }

    private float rowButton(DrawContext ctx, int mx, int my, float x, float y, float w, ButtonSetting bs, float a, boolean inView) {
        rowFrame(ctx, x, y, w, a);
        ctx.drawText(textRenderer, Lang.tr(bs.getName()), (int) x + 8, (int) y + 8, t2(Theme.TEXT, a), false);
        float bw = Math.max(70, textRenderer.getWidth(Lang.tr(bs.get())) + 16);
        button(ctx, mx, my, x + w - bw - 8, y + 4, bw, Lang.tr(bs.get()), a, inView, () -> { bs.run(); ConfigManager.save(); });
        return y + 26;
    }

    private void button(DrawContext ctx, int mx, int my, float x, float y, float w, String label, float a, boolean inView, Runnable action) {
        boolean hov = inView && Render2D.hover(mx, my, x, y, w, 16);
        Render2D.roundRect(ctx, x, y, w, 16, 4, f2(hov ? Theme.ACCENT : Theme.ACCENT_DARK, a));
        Render2D.centered(ctx, label, x + w / 2, y + 4, t2(0xFFFFFFFF, a));
        if (inView) hit(x, y, w, 16, b -> action.run());
    }

    private void renderParticles(DrawContext ctx, float x0, float y0, float W, float H) {
        // пузырьки поднимаются со дна панели и покачиваются
        if (random.nextFloat() < dt * 22 && particles.size() < 70) {
            float life = 3f + random.nextFloat() * 3;
            particles.add(new float[]{random.nextFloat() * W, H + 4, 0, -(14 + random.nextFloat() * 20), life, life, 1 + random.nextInt(3), random.nextFloat() * 6});
        }
        Iterator<float[]> it = particles.iterator();
        while (it.hasNext()) {
            float[] p = it.next();
            p[4] -= dt;
            if (p[4] <= 0 || p[1] < -6) { it.remove(); continue; }
            p[0] += (float) Math.sin((p[4] + p[7]) * 2.2) * 8 * dt;
            p[1] += p[3] * dt;
            float lifeT = p[4] / p[5];
            int al = (int) (150 * Math.min(1, lifeT * 2) * Math.min(1, (1 - lifeT) * 4));
            float r = p[6];
            float bx = x0 + p[0], by = y0 + p[1];
            Render2D.circle(ctx, bx, by, r + 0.8f, f(ColorUtil.withAlpha(0x9FF6FF, al)));
            Render2D.circle(ctx, bx, by, r, f(ColorUtil.withAlpha(0x0B2A42, al / 2)));
            Render2D.rect(ctx, bx - r * 0.5f, by - r * 0.5f, 1, 1, f(ColorUtil.withAlpha(0xFFFFFF, al)));
        }
    }

    private void toggleFx(Module m, float x, float y) {
        m.toggle();
        if (m.isEnabled()) {
            bursts.add(new float[]{x, y, 1});
            for (int k = 0; k < 8; k++) {
                float life = 0.8f + random.nextFloat() * 0.6f;
                particles.add(new float[]{x - lastX0, y - lastY0, 0, -(30 + random.nextFloat() * 30), life, life, 1 + random.nextInt(2), random.nextFloat() * 6});
            }
        }
    }

    // ===================================================================== HELPERS
    private Anim anim(Object key, float target, float speed) {
        Anim a = anims.computeIfAbsent(key, k -> new Anim(target == 1 && key instanceof String s && s.endsWith("#appear") ? 0 : target));
        a.to(target);
        a.update(dt, speed);
        return a;
    }

    private void hit(float x, float y, float w, float h, IntConsumer click) { hits.add(new Hit(x, y, w, h, click, null)); }
    private void hitDrag(float x, float y, float w, float h, DoubleConsumer drag) { hits.add(new Hit(x, y, w, h, null, drag)); }

    private int f(int c) { return ColorUtil.fade(c, alpha); }
    private int f2(int c, float a) { return ColorUtil.fade(c, alpha * a); }
    private int t(int c) { return ColorUtil.textFade(c, alpha); }
    private int t2(int c, float a) { return ColorUtil.textFade(c, alpha * a); }

    private void openSettings(Module m) {
        settingsModule = m;
        expandedColor = null;
        bindingModule = null;
        editingString = null;
        bindingKeySetting = null;
        scrollTarget = 0;
        scroll.snap(0);
    }

    private void closeSettings() {
        editingString = null;
        bindingKeySetting = null;
        settingsModule = null;
        expandedColor = null;
        bindingModule = null;
        scrollTarget = 0;
        scroll.snap(0);
        ConfigManager.save();
    }

    // ===================================================================== INPUT
    //#if MC >= 1.21.9
    // 1.21.9+: события мыши и клавиатуры приходят объектами Click / KeyInput / CharInput
    @Override public boolean mouseClicked(net.minecraft.client.gui.Click c, boolean doubled) { return aquaClick(V.mouseX(), V.mouseY(), c.button()) || super.mouseClicked(c, doubled); }
    @Override public boolean mouseDragged(net.minecraft.client.gui.Click c, double dx, double dy) { return aquaDrag(V.mouseX(), V.mouseY()) || super.mouseDragged(c, dx, dy); }
    @Override public boolean mouseReleased(net.minecraft.client.gui.Click c) { return aquaRelease() || super.mouseReleased(c); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyInput k) { return aquaKey(k.getKeycode(), V.mods(k)) || super.keyPressed(k); }
    @Override public boolean charTyped(net.minecraft.client.input.CharInput c) { boolean r = false; for (char ch : c.asString().toCharArray()) r |= aquaChar(ch); return r || super.charTyped(c); }
    //#else
    @Override public boolean mouseClicked(double mx, double my, int button) { return aquaClick(mx, my, button) || super.mouseClicked(mx, my, button); }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { return aquaDrag(mx, my) || super.mouseDragged(mx, my, button, dx, dy); }
    @Override public boolean mouseReleased(double mx, double my, int button) { return aquaRelease() || super.mouseReleased(mx, my, button); }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { return aquaKey(keyCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers); }
    @Override public boolean charTyped(char chr, int modifiers) { return aquaChar(chr) || super.charTyped(chr, modifiers); }
    //#endif

    private boolean aquaClick(double mx, double my, int button) {
        if (closing) return true;
        // привязка на кнопку мыши: колёсико или боковые кнопки
        if (bindingModule != null && button >= 2) {
            bindingModule.setKey(Keys.MOUSE + button);
            bindingModule = null;
            ConfigManager.save();
            return true;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!Render2D.hover(mx, my, h.x, h.y, h.w, h.h)) continue;
            if (h.drag != null && button == 0) {
                activeDrag = h.drag;
                h.drag.accept(mx);
            } else if (h.click != null) {
                h.click.accept(button);
            } else continue;
            return true;
        }
        if (button == 3 && settingsModule != null) { closeSettings(); return true; } // кнопка «назад» на мыши
        return false;
    }

    private boolean aquaDrag(double mx, double my) {
        if (activeDrag != null) { activeDrag.accept(mx); return true; }
        return false;
    }

    private boolean aquaRelease() {
        if (activeDrag != null) { activeDrag = null; ConfigManager.save(); return true; }
        return false;
    }

    @Override
    //#if MC >= 1.20.2
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
    //#else
    public boolean mouseScrolled(double mx, double my, double vertical) {
    //#endif
        scrollTarget -= (float) vertical * 28;
        scrollTarget = Math.max(0, Math.min(scrollTarget, Math.max(0, contentHeight - viewHeight)));
        return true;
    }

    private boolean aquaKey(int keyCode, int modifiers) {
        if (bindingKeySetting != null) {
            bindingKeySetting.set(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode);
            bindingKeySetting = null;
            ConfigManager.save();
            return true;
        }
        if (editingString != null) {
            String v = editingString.get();
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !v.isEmpty()) editingString.set(v.substring(0, v.length() - 1));
            else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                boolean ok = keyCode != GLFW.GLFW_KEY_ESCAPE;
                Runnable c = profileCommit;
                boolean wasProfile = editingString == profileBuffer;
                editingString = null; profileCommit = null; renameTarget = null;
                if (wasProfile) { if (ok && c != null) c.run(); }
                else ConfigManager.save();
            }
            else if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && client != null) editingString.set(v + client.keyboard.getClipboard());
            return true;
        }
        if (bindingModule != null) {
            bindingModule.setKey(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode);
            bindingModule = null;
            ConfigManager.save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (settingsModule != null) closeSettings();
            else if (!search.isEmpty()) search = "";
            else close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && search.isEmpty()) { close(); return true; }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            scrollTarget = 0;
            return true;
        }
        return false;
    }

    private boolean aquaChar(char chr) {
        if (editingString != null) {
            if (!Character.isISOControl(chr)) editingString.set(editingString.get() + chr);
            return true;
        }
        if (bindingModule == null && settingsModule == null && search.length() < 24 && !Character.isISOControl(chr)) {
            search += chr;
            scrollTarget = 0;
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            ConfigManager.save();
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
