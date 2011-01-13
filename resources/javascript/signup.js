var $j = jQuery.noConflict();

$j.fn.passwordStrength = function( options ){
	return this.each(function(){
		var that = this;that.opts = {};
		that.opts = $j.extend({}, $j.fn.passwordStrength.defaults, options);

		that.div = $j(that.opts.targetDiv);
		that.defaultClass = that.div.attr('class');

		that.percents = (that.opts.classes.length) ? 100 / that.opts.classes.length : 100;

		 v = $j(this)
		.keyup(function(){
			if( typeof el == "undefined" )
				this.el = $j(this);
			var s = getPasswordStrength (this.value);
			var p = this.percents;
			var t = Math.floor( s / p );
			if( 100 <= s )
				t = this.opts.classes.length - 1;

			this.div
				.removeAttr('class')
				.addClass( this.defaultClass )
				.addClass( this.opts.classes[ t ] );

		})
	});

	function getPasswordStrength(H){
		var D=(H.length);
		if (D<4) { D=0 }
		if(D>5){
			D=5
		}
		var F=H.replace(/[0-9]/g,"");
		var G=(H.length-F.length);
		if(G>3){G=3}
		var A=H.replace(/\W/g,"");
		var C=(H.length-A.length);
		if(C>3){C=3}
		var B=H.replace(/[A-Z]/g,"");
		var I=(H.length-B.length);
		if(I>3){I=3}
		var E=((D*10)-20)+(G*10)+(C*15)+(I*10);
		if(E<0){E=0}
		if(E>100){E=100}
		return E
	}

	function randomPassword() {
		var chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$_+";
		var size = 10;
		var i = 1;
		var ret = ""
		while ( i <= size ) {
			$j.max = chars.length-1;
			$j.num = Math.floor(Math.random()*$j.max);
			$j.temp = chars.substr($j.num, 1);
			ret += $j.temp;
			i++;
		}
		return ret;
	}

};

$j(document)
.ready(function(){
	$j('input[name="activate:new_passw"]').passwordStrength({targetDiv: '#iSM',classes : Array('weak','medium','strong')});

});
