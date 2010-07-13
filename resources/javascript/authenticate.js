var $j = jQuery.noConflict();

$j(document).ready(function() {
	$j('div.buttons a').click(function(event) {
		var link = $j(this);
		var action = link.attr('href');
		
		$j('form.authenticateRealm input[name="prm_action"]').val(action);
		
		link.parents('form.authenticateRealm').submit();
		event.preventDefault();
	});
});