package vn.iotstar.services;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service xử lý JSON Web Token sử dụng thư viện Nimbus JOSE + JWT (com.nimbusds:nimbus-jose-jwt)
 */
@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, JWTClaimsSet::getSubject);
    }

    public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
        final JWTClaimsSet claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        try {
            Date issueTime = new Date(System.currentTimeMillis());
            Date expirationTime = new Date(System.currentTimeMillis() + expiration);

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userDetails.getUsername())
                    .issueTime(issueTime)
                    .expirationTime(expirationTime);

            if (extraClaims != null) {
                for (Map.Entry<String, Object> entry : extraClaims.entrySet()) {
                    claimsBuilder.claim(entry.getKey(), entry.getValue());
                }
            }

            JWTClaimsSet claimsSet = claimsBuilder.build();

            // Khởi tạo JWS Header với thuật toán HMAC-SHA256 (HS256)
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);

            // Ký token với MACSigner và secret key
            JWSSigner signer = new MACSigner(getSigningKeyBytes());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Lỗi khi ký JWT bằng Nimbus: " + e.getMessage(), e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null && expiration.before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, JWTClaimsSet::getExpirationTime);
    }

    private JWTClaimsSet extractAllClaims(String token) {
        try {
            // Phân tích cú pháp chuỗi JWT bằng Nimbus SignedJWT
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Xác minh chữ ký bằng MACVerifier
            JWSVerifier verifier = new MACVerifier(getSigningKeyBytes());
            if (!signedJWT.verify(verifier)) {
                throw new SignatureException("The JWT signature is invalid");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // Kiểm tra token đã hết hạn hay chưa
            if (claimsSet.getExpirationTime() != null && claimsSet.getExpirationTime().before(new Date())) {
                throw new RuntimeException("The JWT token has expired");
            }

            return claimsSet;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid compact JWT string: " + e.getMessage(), e);
        } catch (SignatureException e) {
            throw new RuntimeException("The JWT signature is invalid", e);
        } catch (JOSEException e) {
            throw new RuntimeException("Nimbus JOSE exception: " + e.getMessage(), e);
        }
    }

    private byte[] getSigningKeyBytes() {
        try {
            return Base64.getDecoder().decode(secretKey);
        } catch (IllegalArgumentException e) {
            return secretKey.getBytes(StandardCharsets.UTF_8);
        }
    }
}
