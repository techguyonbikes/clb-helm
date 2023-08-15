package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.SiteEnum;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.service.base.GenericCrawService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

@ClbService(componentType = AppConstant.BET_FLUX)
@Slf4j
public class BetFluxCrawlService extends GenericCrawService {

    @Override
    public String getMeetingQueryURI() {
        return AppConstant.BET_FLUX_MEETING_QUERY;
    }

    @Override
    public String getRaceQueryURI() {
        return AppConstant.BET_FLUX_RACE_QUERY;
    }

    @Override
    public SiteEnum getSiteEnum() {
        return SiteEnum.BET_FLUX;
    }

    @Override
    public String getValidCodePrice() {
        return AppConstant.VALID_CHECK_PRODUCT_CODE_BET_FLUX;
    }

    public BetFluxCrawlService(CrawUtils crawUtils, WebClient betFluxWebClient) {
        super(crawUtils, betFluxWebClient);
    }
}
