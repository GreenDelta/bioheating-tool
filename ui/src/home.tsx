import React from "react";
import { useTranslation } from "react-i18next";
import { BreadcrumbRow } from "./components/navi";

export const HomePage = () => {
	const { t } = useTranslation();
	return (
		<>
			<div className="container-fluid">
				<BreadcrumbRow active={`/ ${t("common.home")}`} />
				<div className="my-3">
					<strong>{t("home.welcome")}</strong>
				</div>
				<div className="row">
					<div className="col-md-7">
						<TextBlock />
					</div>

					<div className="col-md-5">
						<img
							src="/img/home.png"
							alt="BIOHEATING"
							className="img-fluid rounded shadow mb-3"
						/>
					</div>
				</div>
			</div>
		</>
	);
};

const TextBlock = () => {
	const { t } = useTranslation();
	return (
		<>
			<p>{t("home.p1")}</p>
			<p>{t("home.p2")}</p>
			<p>{t("home.p3")}</p>
			<p>{t("home.p4")}</p>
			<p>{t("home.p5")}</p>
		</>
	);
};
