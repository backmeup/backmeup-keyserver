package org.backmeup.keyserver.dal;

import org.backmeup.keysrv.worker.Token;

public interface TokenDao {
	public long insertToken(Token token);

	public Token getTokenData(long token_id, String token_pwd);
}
