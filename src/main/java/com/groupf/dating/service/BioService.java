package com.groupf.dating.service;

import com.groupf.dating.dto.BioRewriteRequest;
import com.groupf.dating.dto.BioRewriteResponse;

/**
 * Service interface for bio rewriting functionality
 */
public interface BioService {

    /**
     * Rewrites a bio into multiple versions based on the specified tone
     *
     * @param request the bio rewrite request containing the original bio and tone preference
     * @return the bio rewrite response with multiple versions
     */
    BioRewriteResponse rewriteBio(BioRewriteRequest request);
}
