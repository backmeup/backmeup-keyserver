package org.backmeup.keyserver.dal;

import org.backmeup.keysrv.worker.Token;

public interface TokenDao {
	long insertToken(Token token);

	Token getTokenData(long tokenId, String tokenPwd);
}
