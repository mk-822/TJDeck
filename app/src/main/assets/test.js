!!document.querySelector(".login-container").style.display.match("none") && typeof(tj_deck) == "undefined";

javascript:function tjScrollToTop() {
		var $clm = tj_deck.getClosestColumn(tj_deck.wrapL);
		var $backToHome = $clm.querySelector(".js-column-header");
		if ($backToHome) {
			$backToHome.click();
			return;
		}
}