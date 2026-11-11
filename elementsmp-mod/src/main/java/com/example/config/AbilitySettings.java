package com.example.config;

/**
 * Configurable values of an ability. Which values are used depends on the ability.
 */
public class AbilitySettings {
	public double cooldownSeconds = 10;
	public double power = 1;
	public double radius = 5;
	public double durationSeconds = 5;
	public double damage = 0;
	public double selfDamage = 0;
	/** Player kills needed to use this ability (0 = none). */
	public int killsRequired = 0;
	/** Targeting range in blocks (0 = not used). */
	public double range = 0;
	/** Number of blasts/projectiles (0 = not used). */
	public int count = 0;

	public static AbilitySettings of(double cooldown, double power, double radius, double duration, double damage, double selfDamage) {
		AbilitySettings s = new AbilitySettings();
		s.cooldownSeconds = cooldown;
		s.power = power;
		s.radius = radius;
		s.durationSeconds = duration;
		s.damage = damage;
		s.selfDamage = selfDamage;
		return s;
	}

	public AbilitySettings kills(int required) {
		this.killsRequired = required;
		return this;
	}

	public AbilitySettings range(double value) {
		this.range = value;
		return this;
	}

	public AbilitySettings count(int value) {
		this.count = value;
		return this;
	}

	public AbilitySettings copy() {
		return of(cooldownSeconds, power, radius, durationSeconds, damage, selfDamage)
			.kills(killsRequired).range(range).count(count);
	}
}
