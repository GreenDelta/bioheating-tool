import React from "react";
import { Trans, useTranslation } from "react-i18next";
import { FluidBreadcrumbRow } from "./components/navi";

const PROJECT_URL =
	"https://biogas.fnr.de/projektfoerderung/ausgewaehlte-projekte/projekte/bioheating-tool-fuer-standortspezifische-waermekonzepte";
const SOPHENA_URL = "https://github.com/GreenDelta/Sophena";

/// The about page with general information about the tool; it is accessible
/// without login.
export const AboutPage = () => {
	const { t } = useTranslation();
	return (
		<div className="container-fluid">
			<FluidBreadcrumbRow
				active={t("about.title")}
				path={[["/", t("common.home")]]}
			/>
			<div className="row justify-content-center">
				<div className="col-12 col-md-10 col-lg-8">
					<h3 className="mb-3">{t("about.title")}</h3>
					<p>
						<Trans
							i18nKey="about.intro"
							components={{ 1: <ExternalLink href={PROJECT_URL} /> }}
						/>
					</p>

					<h5 className="border-bottom pb-2 mt-4">{t("about.features")}</h5>
					<ul>
						<li>{t("about.featureCityGml")}</li>
						<li>{t("about.featureOsm")}</li>
						<li>{t("about.featurePrediction")}</li>
						<li>{t("about.featureMap")}</li>
						<li>{t("about.featureNetwork")}</li>
					</ul>

					<h5 className="border-bottom pb-2 mt-4">{t("about.sophena")}</h5>
					<p>
						<Trans
							i18nKey="about.sophenaText"
							components={{ 1: <ExternalLink href={SOPHENA_URL} /> }}
						/>
					</p>

					<h5 className="border-bottom pb-2 mt-4">{t("about.license")}</h5>
					<p>{t("about.licenseText")}</p>
				</div>
			</div>
		</div>
	);
};

const ExternalLink = ({
	href,
	children,
}: {
	href: string;
	children?: React.ReactNode;
}) => (
	<a href={href} target="_blank" rel="noopener noreferrer">
		{children}
	</a>
);
