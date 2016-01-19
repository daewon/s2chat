$(document).ready(function() {
  var format = function(format) {
    var args = Array.prototype.slice.call(arguments, 1);

    return format.replace(/{(\d+)}/g, function(match, number) {
      return typeof args[number] != 'undefined' ? args[number] : match;
    });
  };

  var option = { debug: true, reconnectInterval: 1000 };
  var ws = new ReconnectingWebSocket(common.wsUrl, null, option);

  // view elements
  var welChatList = $("#chat-list");
  var welInputForm = $("#user-input-form");
  var welUserInput = $("#user-input-form .comment");

  function sendMessage(msg) {
    var jsonMsg = JSON.stringify({
      user: common.user,
      roomId: common.room.toString(),
      text: msg
    });

    ws.send(jsonMsg);
  }

  ws.onopen = function(e) {
    console.log('on open', e);
    var msg = { msg: "connected!" };
  };

  ws.onclose = function(e, reason) {
    console.log('on close', e);
  };

  ws.onconnect = function(e) {
    console.log('on connect', e);
  };

  ws.onmessage = function(e) {
    console.log('on message', e.data);

    var msg = JSON.parse(e.data);
    if (msg.user == common.user) {
      var template = format("<li class='message-mine'><span class='name'>{0}</span>: <span>{1}</span></li>", msg.user, msg.text);
    } else {
      var template = format("<li><span>{0}<span>: <span>{1}</span></li>", msg.user, msg.text);
    }

    welChatList.append($(template));
    welChatList.scrollTop(welChatList.prop("scrollHeight"));
  };

 welInputForm.submit(function() {
    var msg = welUserInput.val();

    sendMessage(msg);
    welUserInput.val("");

    return false;
  });
});
