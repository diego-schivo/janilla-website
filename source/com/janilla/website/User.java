/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.website;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.janilla.cms.Document;
import com.janilla.persistence.Index;
import com.janilla.persistence.Store;

@Store
public record User(Long id, String name, @Index String email, String salt, String hash,
		@Index String resetPasswordToken, Instant resetPasswordExpiration, Instant createdAt, Instant updatedAt,
		Document.Status documentStatus, Instant publishedAt) implements Document {

	private static final SecretKeyFactory SECRET;

	private static final Random RANDOM = new SecureRandom();

	static {
		try {
			SECRET = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] hash(char[] password, byte[] salt) {
		var ks = new PBEKeySpec(password, salt, 10000, 512);
		Key k;
		try {
			k = SECRET.generateSecret(ks);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
		return k.getEncoded();
	}

	public boolean passwordEquals(String password) {
		var f = HexFormat.of();
		var s = f.parseHex(salt);
		var h = hash(password.toCharArray(), s);
		return f.formatHex(h).equals(hash);
	}

	public User withPassword(String password) {
		var s = new byte[16];
		RANDOM.nextBytes(s);
		var h = hash(password.toCharArray(), s);
		var f = HexFormat.of();
		return new User(id, name, email, f.formatHex(s), f.formatHex(h), resetPasswordToken, resetPasswordExpiration,
				createdAt, updatedAt, documentStatus, publishedAt);
	}

	public User withResetPassword(String resetPasswordToken, Instant resetPasswordExpiration) {
		return new User(id, name, email, salt, hash, resetPasswordToken, resetPasswordExpiration, createdAt, updatedAt,
				documentStatus, publishedAt);
	}
}
