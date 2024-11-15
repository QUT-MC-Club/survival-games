package supercoder79.survivalgames.game;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;

public final class SurvivalGamesBar {
	private final BossBarWidget widget;

	private SurvivalGamesBar(BossBarWidget widget) {
		this.widget = widget;
	}

	public static SurvivalGamesBar create(GlobalWidgets widgets) {
		return new SurvivalGamesBar(widgets.addBossBar(Text.literal("Worldborder safe! Shrinking in ..."), BossBar.Color.BLUE, BossBar.Style.PROGRESS));
	}

	public void tickSafe(long ticks, long totalTicks) {
		String time = formatTime(ticks);

		this.widget.setTitle(Text.literal("Worldborder safe! Shrinking in " + time));
		this.widget.setProgress((float) ticks / totalTicks);
	}

	public void tickActive(long ticks, long totalTicks) {
		String time = formatTime(ticks);

		this.widget.setTitle(Text.literal("Worldborder shrinking! Finished in " + time));
		this.widget.setProgress((float) ticks / totalTicks);
	}

	public void setFinished() {
		this.widget.setStyle(BossBar.Color.GREEN, BossBar.Style.PROGRESS);
		this.widget.setTitle(Text.literal("Worldborder finished. Fight!"));
		this.widget.setProgress(1.0f);
	}

	public void setActive() {
		this.widget.setStyle(BossBar.Color.RED, BossBar.Style.PROGRESS);
	}

	private static String formatTime(long ticksUntil) {
		long secondsUntil = ticksUntil / 20;

		long minutes = secondsUntil / 60;
		long seconds = secondsUntil % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}
}
